package com.example.application.views.itemlist;

import com.example.application.data.entity.InventoryList;
import com.example.application.data.service.InventoryListService;
import com.example.application.views.MainLayout;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Item List")
@Route(value = "master-detail/:inventoryListID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class ItemListView extends Div implements BeforeEnterObserver {

    private final String INVENTORYLIST_ID = "inventoryListID";
    private final String INVENTORYLIST_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private final Grid<InventoryList> grid = new Grid<>(InventoryList.class, false);

    CollaborationAvatarGroup avatarGroup;

    private TextField itemName;
    private TextField itemCategory;
    private TextField itemCounter;
    private Upload itemImage;
    private Image itemImagePreview;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final CollaborationBinder<InventoryList> binder;

    private InventoryList inventoryList;

    private final InventoryListService inventoryListService;

    public ItemListView(InventoryListService inventoryListService) {
        this.inventoryListService = inventoryListService;
        addClassNames("item-list-view");

        // UserInfo is used by Collaboration Engine and is used to share details
        // of users to each other to able collaboration. Replace this with
        // information about the actual user that is logged, providing a user
        // identifier, and the user's real name. You can also provide the users
        // avatar by passing an url to the image as a third parameter, or by
        // configuring an `ImageProvider` to `avatarGroup`.
        UserInfo userInfo = new UserInfo(UUID.randomUUID().toString(), "Steve Lange");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        avatarGroup = new CollaborationAvatarGroup(userInfo, null);
        avatarGroup.getStyle().set("visibility", "hidden");

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("itemName").setAutoWidth(true);
        grid.addColumn("itemCategory").setAutoWidth(true);
        grid.addColumn("itemCounter").setAutoWidth(true);
        LitRenderer<InventoryList> itemImageRenderer = LitRenderer
                .<InventoryList>of("<img style='height: 64px' src=${item.itemImage} />")
                .withProperty("itemImage", item -> {
                    if (item != null && item.getItemImage() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getItemImage());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(itemImageRenderer).setHeader("Item Image").setWidth("68px").setFlexGrow(0);

        grid.setItems(query -> inventoryListService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(INVENTORYLIST_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ItemListView.class);
            }
        });

        // Configure Form
        binder = new CollaborationBinder<>(InventoryList.class, userInfo);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(itemCounter, String.class)
                .withConverter(new StringToIntegerConverter("Only numbers are allowed")).bind("itemCounter");

        binder.bindInstanceFields(this);

        attachImageUpload(itemImage, itemImagePreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.inventoryList == null) {
                    this.inventoryList = new InventoryList();
                }
                binder.writeBean(this.inventoryList);
                inventoryListService.update(this.inventoryList);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(ItemListView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> inventoryListId = event.getRouteParameters().get(INVENTORYLIST_ID).map(Long::parseLong);
        if (inventoryListId.isPresent()) {
            Optional<InventoryList> inventoryListFromBackend = inventoryListService.get(inventoryListId.get());
            if (inventoryListFromBackend.isPresent()) {
                populateForm(inventoryListFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested inventoryList was not found, ID = %d", inventoryListId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(ItemListView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        itemName = new TextField("Item Name");
        itemCategory = new TextField("Item Category");
        itemCounter = new TextField("Item Counter");
        Label itemImageLabel = new Label("Item Image");
        itemImagePreview = new Image();
        itemImagePreview.setWidth("100%");
        itemImage = new Upload();
        itemImage.getStyle().set("box-sizing", "border-box");
        itemImage.getElement().appendChild(itemImagePreview.getElement());
        formLayout.add(itemName, itemCategory, itemCounter, itemImageLabel, itemImage);

        editorDiv.add(avatarGroup, formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.inventoryList == null) {
                this.inventoryList = new InventoryList();
            }
            this.inventoryList.setItemImage(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(InventoryList value) {
        this.inventoryList = value;
        String topic = null;
        if (this.inventoryList != null && this.inventoryList.getId() != null) {
            topic = "inventoryList/" + this.inventoryList.getId();
            avatarGroup.getStyle().set("visibility", "visible");
        } else {
            avatarGroup.getStyle().set("visibility", "hidden");
        }
        binder.setTopic(topic, () -> this.inventoryList);
        avatarGroup.setTopic(topic);
        this.itemImagePreview.setVisible(value != null);
        if (value == null || value.getItemImage() == null) {
            this.itemImage.clearFileList();
            this.itemImagePreview.setSrc("");
        } else {
            this.itemImagePreview
                    .setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getItemImage()));
        }

    }
}
