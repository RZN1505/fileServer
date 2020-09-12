package messages;

public class LoginMsg extends AbstractMsg {
    private String login;
    private String password;
    private String nickname;

    public LoginMsg(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public LoginMsg(String nickname) {
        this.nickname = nickname;
    }


    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }


}
