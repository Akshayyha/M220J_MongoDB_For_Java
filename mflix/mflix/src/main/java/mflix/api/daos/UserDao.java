package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.print.Doc;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Document> sessionsCollection;

    private final Logger log;

    Session session;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions");
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!

        usersCollection.withWriteConcern(WriteConcern.MAJORITY);
        User userCheck = getUser(user.getEmail());

        if(userCheck!=null && userCheck.getEmail()==user.getEmail()) {
            //TODO > Ticket: Handling Errors - make sure to only add new users
            // and not users that already exist.

            throw new IncorrectDaoOperation("User exists");
        }
        usersCollection.insertOne(user);
        return true;

    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
        session = new Session();
        session.setJwt(jwt);

        Document queryFilter = new Document("user_id",userId);
//        queryFilter.put("jwt",jwt);

        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
        if(sessionsCollection.find(queryFilter).iterator().hasNext()){
            deleteUserSessions(userId);
        }
        session.setUserId(userId);
        sessionsCollection.insertOne(queryFilter);

        return true;
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        Document queryFilter = new Document("email", email);
        User user = usersCollection.find(queryFilter).first();

        return user;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
        if(session!=null)
            session.setUserId(userId);
        return session;
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
        Document queryFilter = new Document("user_id",userId);
        sessionsCollection.deleteMany(queryFilter);
        return true;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        //TODO> Ticket: User Management - implement the delete user method
        Document queryFilter = new Document("email",email);
        usersCollection.deleteOne(queryFilter);


        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.

        return deleteUserSessions(email);
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, String> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
//        Document queryFilter = new Document("email", email);
//        User user = new User();
//        user.setEmail(email);
//        if (userPreferences != null){
//            for (String key : userPreferences.keySet()) {
//                user.setPreferences(userPreferences);
//                usersCollection.replaceOne(queryFilter,user);
//            }
//        }else if(userPreferences==null || userPreferences.isEmpty()) {
//            //TODO > Ticket: Handling Errors - make this method more robust by
//            // handling potential exceptions when updating an entry.
//            throw new IncorrectDaoOperation("Preferences can not be empty or null.", new Throwable());
//        }

        Document queryFilter = new Document("email", email);
        Document userPref = new Document();

        if (userPreferences == null || userPreferences.isEmpty()) {
            throw new IncorrectDaoOperation("Preferences can not be empty or null.", new Throwable());
        }

        for (String key : userPreferences.keySet()) {
            userPref.put(key, userPreferences.get(key).toString());
        }

        usersCollection.updateOne(Filters.eq("email", email), set("preferences", userPref),
                new UpdateOptions().upsert(true));

        return true;
    }
}
