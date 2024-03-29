package com.example.lab7.gui;

import com.example.lab7.Session;
import com.example.lab7.domain.Utilizator;
import com.example.lab7.repository.paging.Page;
import com.example.lab7.repository.paging.Pageable;
import com.example.lab7.repository.paging.PageableImplementation;
import com.example.lab7.service.Service;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UtilizatorController {

    Service service;
    ObservableList<Utilizator> model = FXCollections.observableArrayList();
    ObservableList<Utilizator> modelFriends = FXCollections.observableArrayList();
    private FadeTransition fadeOut;
    private Utilizator loggedUser;
    private Page<Utilizator> currentPageUsers;
    private Page<Utilizator> currentPageFriends;

    @FXML
    TableView<Utilizator> tableView;
    @FXML
    TableColumn<Utilizator, String> tableColumnNume;
    @FXML
    TableColumn<Utilizator, String> tableColumnPrenume;
    @FXML
    TableColumn<Utilizator, String> tableColumnUsername;
    @FXML
    TableColumn<Utilizator, String> tableColumnEmail;
    @FXML
    private TextField dataForSearch;
    @FXML
    private TextField numberOfUsersField;
    @FXML
    public TextField currentPageNumberField;
    @FXML
    private Text maximumNumberOfPagesText;
    @FXML
    private Label errorMessageLabel;

    private String whichTable;


    public void setService(Service service) {
        this.service = service;
        initModel();
    }

    @FXML
    public void initialize() {
        whichTable = "Users";
        this.loggedUser = Session.getLoggedUser();

        tableColumnNume.setCellValueFactory(new PropertyValueFactory<Utilizator, String>("nume"));
        tableColumnPrenume.setCellValueFactory(new PropertyValueFactory<Utilizator, String>("prenume"));
        tableColumnUsername.setCellValueFactory(new PropertyValueFactory<Utilizator, String>("username"));
        tableColumnEmail.setCellValueFactory(new PropertyValueFactory<Utilizator, String>("email"));

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setItems(model);
    }

    @FXML
    private void handlePreviousPage(ActionEvent event) {
        if (Objects.equals(whichTable, "Users")) {
            this.currentPageUsers = service.getPreviousPageUsers();
            initModel();
        } else {
            this.currentPageFriends = service.getPreviousPageFriends();
            initFriendsModel();
        }
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        if (Objects.equals(whichTable, "Users")) {
            this.currentPageUsers = service.getNextPageUsers();
            initModel();
        } else {
            this.currentPageFriends = service.getNextPageFriends();
            initFriendsModel();
        }
    }

    @FXML
    private void handlePageNumberChange(ActionEvent event) {
        try {
            int pageNumber = Integer.parseInt(currentPageNumberField.getText());
            Pageable pageable = new PageableImplementation(pageNumber, Integer.parseInt(numberOfUsersField.getText()));
            if (Objects.equals(whichTable, "Users")) {
                this.currentPageUsers = service.getAllUtilizatoriPaginat(pageable);
                initModel();
            } else {
                this.currentPageFriends = service.getAllPrieteniPaginat(pageable, loggedUser.getId());
                initFriendsModel();
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid page number!", "error-label");
        }
    }

    @FXML
    private void handleNumberOfUsersChange(ActionEvent event) {
        try {
            int numberOfUsers = Integer.parseInt(numberOfUsersField.getText());
            Pageable pageable = new PageableImplementation(1, numberOfUsers);
            if (Objects.equals(whichTable, "Users")) {
                this.currentPageUsers = service.getAllUtilizatoriPaginat(pageable);
                initModel();
            } else {
                this.currentPageFriends = service.getAllPrieteniPaginat(pageable, loggedUser.getId());
                initFriendsModel();
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid number of friends!", "error-label");
        }
    }

    private void initModel() {
        whichTable = "Users";
        this.currentPageUsers = service.getCurrentPageUsers();
        List<Utilizator> content = currentPageUsers.getContent().collect(Collectors.toList());
        int numberOfUsers = (int) StreamSupport.stream(service.getAllUtilizatori().spliterator(), false).count();
        int division = numberOfUsersField.getText().isEmpty() ? 1 :
                (numberOfUsers % Integer.parseInt(numberOfUsersField.getText()) == 0) ?
                        numberOfUsers / Integer.parseInt(numberOfUsersField.getText()) :
                        numberOfUsers / Integer.parseInt(numberOfUsersField.getText()) + 1;
        maximumNumberOfPagesText.setText(String.valueOf(division));
        currentPageNumberField.setText(String.valueOf(currentPageUsers.getPageable().getPageNumber()));
        model.setAll(content);
        tableView.setItems(model);
    }

    private void initFriendsModel() {
//        List<Utilizator> friends = service.getPrieteniUtilizator(loggedUser.getId());
//        List<Utilizator> list = new ArrayList<>(friends);
//        model.setAll(list);
        whichTable = "Friends";
        this.currentPageFriends = service.getCurrentPageFriends();
        List<Utilizator> content = currentPageFriends.getContent().collect(Collectors.toList());
        System.out.println(content.size());
        int numberOfUsers = service.getPrieteniUtilizator(loggedUser.getId()).size();
        int division = numberOfUsersField.getText().isEmpty() ? 1 :
                (numberOfUsers % Integer.parseInt(numberOfUsersField.getText()) == 0) ?
                        numberOfUsers / Integer.parseInt(numberOfUsersField.getText()) :
                        numberOfUsers / Integer.parseInt(numberOfUsersField.getText()) + 1;
        maximumNumberOfPagesText.setText(String.valueOf(division));
        currentPageNumberField.setText(String.valueOf(currentPageFriends.getPageable().getPageNumber()));
        modelFriends.setAll(content);
        tableView.setItems(modelFriends);
    }

    private void initCommunityModel() {
        List<Utilizator> community = service.getCommunityOfLoggedUser();
        model.setAll(community);
    }

    @FXML
    public void handleFindUser(ActionEvent actionEvent) {
        String data = dataForSearch.getText().trim();
        if (data.isEmpty()) {
            showErrorAlert("Introduceti text in casuta de cautare!", "error-label");
            return;
        }
        clearAddFields();
        List<Utilizator> searchResults = service.cautaUtilizatoriDupaCeva(data);
        if (searchResults.isEmpty()) {
            showErrorAlert("Nu s-au gasit utilizatori!", "error-label");
        } else {
            model.setAll(searchResults);
        }
    }

    @FXML
    public void handleAddRequest(ActionEvent actionEvent) {
        try {
            Utilizator selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.addFriendRequest(selected);
                showErrorAlert("Cererea a fost adaugata cu succes!", "success-label");
            } else {
                showErrorAlert("Nu a fost selectat niciun utilizator!", "error-label");
            }
        } catch (Exception e) {
            showErrorAlert("Eroare la adaugarea cererii de prietenie!", "error-label");
        }
    }

    @FXML
    public void handleDeleteFriend(ActionEvent actionEvent) {
        try {
            Utilizator selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.stergePrietenie(selected.getId(), loggedUser.getId());
                showErrorAlert("Prietenul a fost sters cu succes!", "success-label");
                initFriendsModel();
            } else {
                showErrorAlert("Nu a fost selectat niciun utilizator!", "error-label");
            }
        } catch (Exception e) {
            showErrorAlert("Eroare la eliminarea prietenului!", "error-label");
        }
    }

    @FXML
    public void handleUserProfileDetails(ActionEvent actionEvent) {
        Utilizator loggedUser = service.getLoggedUser();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/lab7/gui/views/profile-view.fxml"));
            AnchorPane userProfilePane = loader.load();

            UserProfileController userProfileController = loader.getController();
            userProfileController.setService(service);
            userProfileController.init(loggedUser);

            Stage userProfileStage = new Stage();
            userProfileStage.setTitle("User Profile");
            userProfileStage.initModality(Modality.WINDOW_MODAL);
            userProfileStage.initOwner(tableView.getScene().getWindow());

            Scene userProfileScene = new Scene(userProfilePane);
            userProfileStage.setScene(userProfileScene);

            userProfileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCommunityOfLoggedUser(ActionEvent actionEvent) {
        try {
            initCommunityModel();
        } catch (Exception e) {
            showErrorAlert("Eroare la redarea comunitatii!", "error-label");
        }
    }

    @FXML
    public void handleRefreshTable(ActionEvent actionEvent) {
        try {
            int numberOfUsers = (int) StreamSupport.stream(service.getAllUtilizatori().spliterator(), false).count();
            Pageable pageable = new PageableImplementation(1, numberOfUsers);
            this.currentPageUsers = service.getAllUtilizatoriPaginat(pageable);
            numberOfUsersField.setText(String.valueOf(numberOfUsers));
            initModel();
        } catch (NumberFormatException e) {
            showErrorAlert("Error displaying users!", "error-label");
        }
    }

    @FXML
    public void handleRefreshFriendsTable(ActionEvent actionEvent) {
        try {
            int numberOfUsers = service.getPrieteniUtilizator(loggedUser.getId()).size();
            Pageable pageable = new PageableImplementation(1, numberOfUsers);
            this.currentPageFriends = service.getAllPrieteniPaginat(pageable, loggedUser.getId());
            numberOfUsersField.setText(String.valueOf(numberOfUsers));
            initFriendsModel();
        } catch (NumberFormatException e) {
            showErrorAlert("Error displaying friends!", "error-label");
        }
    }

    private void showErrorAlert(String message, String styleClass) {
        errorMessageLabel.setText(message);
        errorMessageLabel.getStyleClass().setAll(styleClass);
        errorMessageLabel.setVisible(true);

        fadeOut = new FadeTransition(Duration.seconds(2), errorMessageLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.play();
    }

    @FXML
    public void openFriendRequestsWindow() {
        try {
            FXMLLoader friendsLoader = new FXMLLoader();
            friendsLoader.setLocation(getClass().getResource("/com/example/lab7/gui/views/friendships-view.fxml"));
            AnchorPane friendsLayout = friendsLoader.load();

            FriendshipsController friendsController = friendsLoader.getController();
            friendsController.setService(service);

            Stage chatStage = new Stage();
            chatStage.setTitle("Friend Requests");
            chatStage.initModality(Modality.WINDOW_MODAL);
            chatStage.setScene(new Scene(friendsLayout));
            chatStage.setResizable(true);
            chatStage.setMaximized(true);

            chatStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openChatWindow() {
        try {
            FXMLLoader chatLoader = new FXMLLoader();
            chatLoader.setLocation(getClass().getResource("/com/example/lab7/gui/views/chat-view2.fxml"));
            BorderPane chatLayout = chatLoader.load();

            ChatController chatController = chatLoader.getController();
            chatController.setService(service);

            Stage chatStage = new Stage();
            chatStage.setTitle("Chat");
            chatStage.initModality(Modality.WINDOW_MODAL);
            chatStage.setScene(new Scene(chatLayout));
            chatStage.setResizable(true);
            chatStage.setMaximized(true);

            chatStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearAddFields() {
        dataForSearch.clear();
    }

    @FXML
    private void handleExit() throws IOException {
        Stage currentStage = (Stage) dataForSearch.getScene().getWindow();
        FXMLLoader loginLoader = new FXMLLoader();
        loginLoader.setLocation(getClass().getResource("/com/example/lab7/gui/views/login-view.fxml"));
        AnchorPane loginLayout = loginLoader.load();
        currentStage.setScene(new Scene(loginLayout));

        LoginController loginController = loginLoader.getController();
        loginController.setService(service);
        loginController.setStage(currentStage);
    }
}