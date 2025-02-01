import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Set;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
class Twitter {
    List<List<String>> news;
    HashMap<String, List<String>> following;
    HashMap<String, Integer> followerCount;

    public Twitter() {
        news = new LinkedList<>();
        following = new HashMap<>();
        followerCount = new HashMap<>();
    }

    public void postTweet(String userId, String tweetId) {
        if (!following.containsKey(userId)) {
            List<String> x = new LinkedList<>();
            x.add(userId);
            following.put(userId, x);
        }
        List<String> obj = new LinkedList<>();
        obj.add(userId);
        obj.add(tweetId);
        news.add(0, obj);
    }

    public List<String> getNewsFeed(String userId) {
        List<String> ans = new LinkedList<>();
        int c = 0;
        if(!following.containsKey(userId))
        return ans;
        for (List<String> ob : news) {
            if (following.get(userId).contains(ob.get(0))) {
                ans.add(ob.get(1));
                if (++c == 10)
                    break;
            }
        }
        return ans;
    }

    public void follow(String followerId, String followeeId) {
        if (!following.containsKey(followerId)) {
            List<String> x = new LinkedList<>();
            x.add(followerId);
            following.put(followerId, x);
        }
        if (!following.get(followerId).contains(followeeId)) {
            following.get(followerId).add(followeeId);
            followerCount.put(followeeId, followerCount.getOrDefault(followeeId, 0) + 1);
        }
    }

    public void unfollow(String followerId, String followeeId) {
        if (following.containsKey(followerId) && !followeeId.equals(followerId)) {
            if (following.get(followerId).remove(followeeId)) {
                followerCount.put(followeeId, followerCount.getOrDefault(followeeId, 0) - 1);
            }
        }
    }

    public int getFollowerCount(String userId) {
        return followerCount.getOrDefault(userId, 0);
    }

    public List<String> recommendUsers(String userId) {
    List<String> recommendedUsers = new LinkedList<>();
    if (!following.containsKey(userId)) {
        following.put(userId, new LinkedList<>());
    }

    // Convert following data into a user-item matrix
    HashMap<String, HashMap<String, Integer>> userFollowMatrix = new HashMap<>();
    for (String user : following.keySet()) {
        HashMap<String, Integer> follows = new HashMap<>();
        for (String followee : following.keySet()) {
            follows.put(followee, following.get(user).contains(followee) ? 1 : 0);
        }
        userFollowMatrix.put(user, follows);
    }

    // Calculate similarity scores between the current user and all others
    HashMap<String, Double> similarityScores = new HashMap<>();
    HashMap<String, Integer> currentUserVector = userFollowMatrix.get(userId);

    for (String otherUser : userFollowMatrix.keySet()) {
        if (!otherUser.equals(userId)) {
            HashMap<String, Integer> otherUserVector = userFollowMatrix.get(otherUser);
            double similarity = cosineSimilarity(currentUserVector, otherUserVector);
            similarityScores.put(otherUser, similarity);
        }
    }

    // Use a set to avoid duplicate recommendations
    Set<String> alreadyRecommended = new HashSet<>();

    // Rank users by similarity and recommend users they follow
    similarityScores.entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by similarity in descending order
        .forEach(entry -> {
            String similarUser = entry.getKey();
            for (String followee : following.get(similarUser)) {
                if (!following.get(userId).contains(followee) && !followee.equals(userId) && alreadyRecommended.add(followee)) {
                    recommendedUsers.add(followee);
                }
            }
        });

    return recommendedUsers;
}

// Helper method to calculate cosine similarity
private double cosineSimilarity(HashMap<String, Integer> vectorA, HashMap<String, Integer> vectorB) {
    int dotProduct = 0;
    double normA = 0.0;
    double normB = 0.0;

    for (String key : vectorA.keySet()) {
        int valA = vectorA.get(key);
        int valB = vectorB.getOrDefault(key, 0);

        dotProduct += valA * valB;
        normA += valA * valA;
        normB += valB * valB;
    }

    return (normA == 0 || normB == 0) ? 0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}



}

public class TwitterApp extends Application {
    private Twitter twitter;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        twitter = new Twitter();

        primaryStage.setTitle("Enhanced Twitter GUI with Follower Count");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #f0f8ff;");

        Label header = new Label("Twitter Interface");
        header.setFont(new Font("Arial", 24));
        header.setTextFill(Color.DARKBLUE);

        Font labelFont = new Font("Arial", 16);

        Label postTweetLabel = new Label("Post Tweet:");
        postTweetLabel.setFont(labelFont);
        TextField postUserIdField = new TextField();
        postUserIdField.setPromptText("User ID");
        TextField postTweetIdField = new TextField();
        postTweetIdField.setPromptText("Tweet ID");
        Button postTweetButton = new Button("Post Tweet");
        postTweetButton.setStyle("-fx-background-color: #5bc0de; -fx-text-fill: white;");

        postTweetButton.setOnAction(e -> {
            String userId = postUserIdField.getText();
            String tweetId = postTweetIdField.getText();
            twitter.postTweet(userId, tweetId);
            showAlert("Tweet Posted", "User " + userId + " posted tweet " + tweetId);
        });

        VBox postTweetSection = new VBox(5, postTweetLabel, postUserIdField, postTweetIdField, postTweetButton);
        postTweetSection.setPadding(new Insets(10));
        postTweetSection.setStyle("-fx-background-color: #e1f5fe; -fx-border-radius: 5; -fx-border-color: #81d4fa;");

        Label newsFeedLabel = new Label("Get News Feed:");
        newsFeedLabel.setFont(labelFont);
        TextField newsFeedUserIdField = new TextField();
        newsFeedUserIdField.setPromptText("User ID");
        Button getNewsFeedButton = new Button("Get News Feed");
        getNewsFeedButton.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white;");
        ListView<String> newsFeedList = new ListView<>();

        getNewsFeedButton.setOnAction(e -> {
            String userId = newsFeedUserIdField.getText();
            List<String> feed = twitter.getNewsFeed(userId);
            newsFeedList.getItems().clear();
            newsFeedList.getItems().addAll(feed);
        });

        VBox newsFeedSection = new VBox(5, newsFeedLabel, newsFeedUserIdField, getNewsFeedButton, newsFeedList);
        newsFeedSection.setPadding(new Insets(10));
        newsFeedSection.setStyle("-fx-background-color: #f1f8e9; -fx-border-radius: 5; -fx-border-color: #aed581;");

        Label followLabel = new Label("Follow User:");
        followLabel.setFont(labelFont);
        TextField followerIdField = new TextField();
        followerIdField.setPromptText("Follower ID");
        TextField followeeIdField = new TextField();
        followeeIdField.setPromptText("Followee ID");
        Button followButton = new Button("Follow");
        followButton.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white;");

        followButton.setOnAction(e -> {
            String followerId = followerIdField.getText();
            String followeeId = followeeIdField.getText();
            twitter.follow(followerId, followeeId);
            showAlert("Followed", "User " + followerId + " followed user " + followeeId);
        });

        VBox followSection = new VBox(5, followLabel, followerIdField, followeeIdField, followButton);
        followSection.setPadding(new Insets(10));
        followSection.setStyle("-fx-background-color: #e8eaf6; -fx-border-radius: 5; -fx-border-color: #7986cb;");

        Label unfollowLabel = new Label("Unfollow User:");
        unfollowLabel.setFont(labelFont);
        TextField unfollowerIdField = new TextField();
        unfollowerIdField.setPromptText("Follower ID");
        TextField unfolloweeIdField = new TextField();
        unfolloweeIdField.setPromptText("Followee ID");
        Button unfollowButton = new Button("Unfollow");
        unfollowButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");

        unfollowButton.setOnAction(e -> {
            String followerId = unfollowerIdField.getText();
            String followeeId = unfolloweeIdField.getText();
            twitter.unfollow(followerId, followeeId);
            showAlert("Unfollowed", "User " + followerId + " unfollowed user " + followeeId);
        });

        VBox unfollowSection = new VBox(5, unfollowLabel, unfollowerIdField, unfolloweeIdField, unfollowButton);
        unfollowSection.setPadding(new Insets(10));
        unfollowSection.setStyle("-fx-background-color: #ffebee; -fx-border-radius: 5; -fx-border-color: #ef9a9a;");

        Label followerCountLabel = new Label("Check Follower Count:");
        followerCountLabel.setFont(labelFont);
        TextField followerCountUserIdField = new TextField();
        followerCountUserIdField.setPromptText("User ID");
        Button getFollowerCountButton = new Button("Get Follower Count");
        getFollowerCountButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        Label followerCountResult = new Label();

        getFollowerCountButton.setOnAction(e -> {
            String userId = followerCountUserIdField.getText();
            int count = twitter.getFollowerCount(userId);
            followerCountResult.setText("User " + userId + " has " + count + " followers.");
        });

        VBox followerCountSection = new VBox(5, followerCountLabel, followerCountUserIdField, getFollowerCountButton, followerCountResult);
        followerCountSection.setPadding(new Insets(10));
        followerCountSection.setStyle("-fx-background-color: #fff3e0; -fx-border-radius: 5; -fx-border-color: #ffb74d;");

        Label recommendUsersLabel = new Label("Recommend Users:");
        recommendUsersLabel.setFont(labelFont);
        TextField recommendUserIdField = new TextField();
        recommendUserIdField.setPromptText("User ID");
        Button recommendButton = new Button("Get Recommendations");
        recommendButton.setStyle("-fx-background-color: #8bc34a; -fx-text-fill: white;");
        ListView<String> recommendationList = new ListView<>();

        recommendButton.setOnAction(e -> {
            String userId = recommendUserIdField.getText();
            List<String> recommendedUsers = twitter.recommendUsers(userId);
            recommendationList.getItems().clear();
            recommendationList.getItems().addAll(recommendedUsers);
        });

        VBox recommendSection = new VBox(5, recommendUsersLabel, recommendUserIdField, recommendButton, recommendationList);
        recommendSection.setPadding(new Insets(10));
        recommendSection.setStyle("-fx-background-color: #dcedc8; -fx-border-radius: 5; -fx-border-color: #a5d6a7;");

        layout.getChildren().addAll(header, postTweetSection, newsFeedSection, followSection, unfollowSection, followerCountSection, recommendSection);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 500, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
