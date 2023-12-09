package agents;

import entity.*;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.google.gson.Gson;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseAgent extends Agent {

    protected void setup() {

        addBehaviour(new FetchQuestionsWithAnswersBehaviour());
    }

    private class FetchQuestionsWithAnswersBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("FetchQuestionsWithAnswers")
            );

            ACLMessage request = myAgent.receive(mt);

            if (request != null) {
                String[] questionsWithAnswers = fetchQuestionsWithAnswersFromDatabase();
                String jsonResponse = new Gson().toJson(questionsWithAnswers);

                System.out.println("Received request for fetching questions with answers");

                ACLMessage reply = request.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setConversationId("FetchQuestionsWithAnswers");
                reply.setContent(jsonResponse);
                myAgent.send(reply);
            } else {
                block();
            }
        }

        private String[] fetchQuestionsWithAnswersFromDatabase() {
            List<QuestionWithAnswers> questionsWithAnswersList = new ArrayList<>();

            // Database connection string
            String dbUrl = "jdbc:mysql://localhost:3306/seng696";
            String dbUser = "root";
            String dbPass = "";

            // SQL query to fetch questions with options and answers
            String query = "SELECT q.id as question_id, q.text as question_text, " +
                    "o.id as option_id, o.is_correct, o.text as option_text " +
                    "FROM questions q " +
                    "JOIN options o ON q.id = o.question_id";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                Map<Integer, QuestionWithAnswers> questionMap = new HashMap<>();

                while (rs.next()) {
                    int questionId = rs.getInt("question_id");
                    String questionText = rs.getString("question_text");

                    int optionId = rs.getInt("option_id");
                    boolean isCorrect = rs.getBoolean("is_correct");
                    String optionText = rs.getString("option_text");

                    // Create or retrieve QuestionWithAnswers object
                    QuestionWithAnswers questionWithAnswers = questionMap.computeIfAbsent(questionId,
                            id -> new QuestionWithAnswers(id, questionText));

                    // Add option to the question
                    questionWithAnswers.addOption(new Option(optionId, isCorrect, optionText));
                }

                // Convert the map values (QuestionWithAnswers objects) to a list
                questionsWithAnswersList.addAll(questionMap.values());

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Convert the list of QuestionWithAnswers objects to an array and return
            return questionsWithAnswersList.stream()
                    .map(questionWithAnswers -> new Gson().toJson(questionWithAnswers))
                    .toArray(String[]::new);
        }
    }


}

