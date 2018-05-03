package group.greenbyte.lunchplanner.user;

import group.greenbyte.lunchplanner.exceptions.DatabaseException;
import group.greenbyte.lunchplanner.exceptions.HttpRequestException;
import group.greenbyte.lunchplanner.security.JwtService;
import group.greenbyte.lunchplanner.user.database.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserLogic {

    private final JwtService jwtService;

    private static final Pattern REGEX_MAIL = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");

    // This variable will be set over the setter Method by java spring
    private UserDao userDao;

    @Autowired
    public UserLogic(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // ------------------ JWT --------------------
    public User createUserToken(String username) {
        String token = jwtService.createToken(username, getExpirationDate());
        try {
            return userDao.setTokenForUser(username, token);
        } catch (DatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public User validateUser(String token) {
        try {
            String username = jwtService.getUsername(token);
            if (username != null ) {
                User user = userDao.getUser(username);

                if (user != null && token.equals(user.getToken()) && jwtService.isValid(token)) {
                    return user;
                }
            }
        } catch (DatabaseException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    public Date getExpirationDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_WEEK, 1);
        return c.getTime();
    }


    // --------------------- JWT ENDE ----------------------

    /**
     * Create a user
     *
     * @param userName his username
     * @param password the unhashed password
     * @param mail mail-adress
     *
     * @throws HttpRequestException when an parameter is not valid or user already exists or an DatabaseError happens
     */
    void createUser(String userName, String password, String mail) throws HttpRequestException {
        if(userName == null || userName.length() == 0)
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "user name is empty");

        if(userName.length() > User.MAX_USERNAME_LENGTH)
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "user name is too long");

        if(password == null || password.length() == 0)
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "password is empty");

        if(mail == null || mail.length() == 0)
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "mail is empty");

        if(mail.length() > User.MAX_MAIL_LENGTH)
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "mail is too long");

        if(!REGEX_MAIL.matcher(mail).matches())
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "mail is not valid");

        try {
            if(userDao.getUser(userName) != null)
                throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), "user name already exists");

            userDao.createUser(userName, SecurityHelper.hashPassword(password), mail);
        } catch (DatabaseException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new HttpRequestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }


    /**
     *
     * @param searchword String for searching the Database
     * @return a List of User in natural sorted order
     * @throws HttpRequestException
     */
    public List<User> searchUserByName(String searchword) throws HttpRequestException{
        try{
            return userDao.searchUserByName(searchword);
        }catch (Exception e){
            throw new HttpRequestException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }



    /**
     *
     * @param userName name of Username for searching in database
     * @return
     * @throws HttpRequestException
     */
    public User getUser(String userName) throws HttpRequestException {
        try {
            return userDao.getUser(userName);
        } catch (DatabaseException e) {
            throw new HttpRequestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    /**
     * Send an invitation to a user (async)
     *
     * @param userName who intivtes
     * @param toInviteUserName who is invited
     */
    public void sendInvitation(String userName, String toInviteUserName) {
        //ToDO send notfication to user
    }


    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
