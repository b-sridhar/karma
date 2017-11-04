package com.karma;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Main {
    public static List<Task> taskList = new ArrayList<Task>();
    public static List<Task> sortedTaskList = new ArrayList<Task>();
    public static Writer htmlWriter = null;
    public static Map<String, String> props = null;

    public static void loadProperties(){
        try {
            props = new HashMap<String, String>();

            JsonReader reader = new JsonReader(new FileReader(".karma.properties"));
            Gson gson = new Gson();
            props = gson.fromJson(reader, Map.class);
            reader.close();

        }catch (Exception ex){
            System.out.println("Unable to load properties from .karma.properties.");
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    public static String post(String restUrl, String username, String password, String query)
    {
        String jsonResponse = null;
        try {
            HttpPost post = new HttpPost(restUrl);
            String auth = new StringBuffer(username).append(":").append(password).toString();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            post.setHeader("AUTHORIZATION", authHeader);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setHeader("X-Stream", "true");

            HttpResponse response=null;
            String line = "";
            StringBuffer result = new StringBuffer();
            post.setEntity(new StringEntity(query));
            HttpClient client = HttpClientBuilder.create().build();
            response = client.execute(post);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((line = reader.readLine()) != null){ result.append(line); }
            jsonResponse = result.toString();

        }catch(Exception ex){
            System.out.println("Unable to fetch tickets from Jira.");
            System.out.println("Exception in post(): " + ex.getMessage());
        }

        return jsonResponse;
    }

    public static void fetchTickets(){
        try{

            String restUrl = props.get("jira_url");;
            String username = props.get("jira_username");
            String password = props.get("jira_password");

            JsonObject jsonQuery = new JsonObject();
            jsonQuery.addProperty("jql", props.get("jira_query"));
            JsonArray qfields = new JsonArray();
            qfields.add("key");
            qfields.add("comment");
            qfields.add("summary");
            qfields.add("status");
            jsonQuery.add("fields", qfields);
            String query = jsonQuery.toString();

            String response = post(restUrl, username, password, query);

            JsonObject root = new JsonParser().parse(response).getAsJsonObject();
            JsonArray issues = root.get("issues").getAsJsonArray();

            for(int i=0; i<issues.size(); i++){

                Task task = new Task();
                JsonObject issue = issues.get(i).getAsJsonObject();
                JsonObject fields = issue.get("fields").getAsJsonObject();

                task.setKey(issue.get("key").getAsString());
                task.setSummary(fields.get("summary").getAsString());
                task.setStatus(fields.get("status").getAsJsonObject().get("name").getAsString());

                JsonArray commentsList = fields.get("comment").getAsJsonObject().get("comments").getAsJsonArray();

                int prevCommentId = 0;
                for(int j=0; j<commentsList.size(); j++){
                    JsonObject comment = commentsList.get(j).getAsJsonObject();

                    int commentId = comment.get("id").getAsInt();
                    if(commentId > prevCommentId) {
                        String author = comment.get("author").getAsJsonObject().get("key").getAsString();
                        if(author.equals(props.get("jira_username"))) {
                            task.setAuthor(author);
                            task.setLastComment(comment.get("body").getAsString());
                        }
                    }
                }

                if(task.getLastComment() == null){
                    task.setLastComment("");
                }

                taskList.add(task);
            }

        }catch (Exception ex){
            System.out.println("Exception in fetchTickets():" + ex.getMessage());
        }
    }

    public static void sortTickets(){
        try{

            String[] statuses = { "Blocked", "CLOSED", "In Acceptance", "In Progress"};
            for(int i=0; i<statuses.length; i++){

                for(int j=0; j<taskList.size(); j++){
                    if(statuses[i].equals(taskList.get(j).getStatus())){
                        sortedTaskList.add(taskList.get(j));
                    }
                }
            }

        }catch (Exception ex){
            System.out.println("Exception in sortTickets(): " + ex.getMessage());
        }
    }

    public static void buildReport(){
        try{
            Configuration cfg = new Configuration();
            cfg.setClassForTemplateLoading(Main.class.getClass(), "/");
            Template template = cfg.getTemplate("report.ftl");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("message", props.get("email_message"));
            data.put("tasks", sortedTaskList);
            data.put("signature", props.get("signature"));

            htmlWriter = new StringWriter();
            template.process(data, htmlWriter);

        }catch(Exception ex){
            System.out.println("Exception in buildReport(): " + ex.getMessage());
        }
    }

    public static void sendEmail(){
        try
        {
            Properties emailProperties = System.getProperties();
            emailProperties.put("mail.smtp.host", props.get("smtp_host"));
            emailProperties.put("mail.smtp.port", props.get("smtp_port"));

            Session session = Session.getInstance(emailProperties, null);
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.setFrom(new InternetAddress(props.get("email_from_address")));
            msg.setReplyTo(InternetAddress.parse(props.get("email_to_address"), false));
            msg.setSubject("Weekly Update", "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(props.get("email_to_address"), false));
            msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(props.get("email_from_address")));

            BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(htmlWriter.toString(), "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(bodyPart);
            msg.setContent(multipart);

            Transport.send(msg);
            System.out.println("EMail Sent Successfully!!");
        }
        catch (Exception ex) {
            System.out.println("Exception in sendEmail(): " + ex.getMessage());
        }
    }

    public static void main(String args[])
    {
        loadProperties();
        fetchTickets();
        sortTickets();
        buildReport();
        sendEmail();

    }
}
