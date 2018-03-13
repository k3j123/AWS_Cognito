package k3j123.aws_cognito;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

public class MainActivity extends AppCompatActivity {

    /*On Screen fields*/
    private EditText username;
    private EditText password;
    private Button submitButton;

    /*User Details*/
    private String mUsername;
    private String mPassword;

    /*Continuations*/
    private ForgotPasswordContinuation mForgotPasswordContinuation;
    private NewPasswordContinuation mNewPasswordContinuation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Initiate AWSHelper here*/
        AWSHelper.init(getApplicationContext());

        /*We will now bind our modules*/
        initApp();

        /*Check to see if a user is already signed in and proceed, else they will need to sign up/sign in*/
        findCurrent();
    }

    /*Binding the model to the view*/
    private void initApp(){
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        submitButton = findViewById(R.id.submitButton);
        setListener();
    }

    /*Check to see if there is a user already logged in*/
    private void findCurrent(){
        CognitoUser user = AWSHelper.getPool().getCurrentUser();
        mUsername = user.getUserId();
        if(mUsername != null){
            AWSHelper.setUser(mUsername);
            username.setText(user.getUserId());
            user.getSessionInBackground(authenticationHandler);
        }
    }

    /*method is sets submitButton up to listen for user click and proceeds to log user in*/
    private void setListener(){
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUsername = username.getText().toString().toLowerCase();
                AWSHelper.setUser(mUsername);

                mPassword = password.getText().toString();
                AWSHelper.getPool().getUser(mUsername).getSessionInBackground(authenticationHandler);
            }
        });
    }

    /*Simple method that clears out username and password fields on screen*/
    private void clearInput() {
        if (username == null) {
            username = findViewById(R.id.username);
        }

        if (password == null) {
            password = findViewById(R.id.password);
        }

        username.setText("");
        username.requestFocus();
        password.setText("");
    }

    /*This method will be used to call each of the main components of Cognito for this app*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                //Register user
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        String name = data.getStringExtra("name");
                        if (!name.isEmpty()) {
                            username.setText(name);
                            password.setText("");
                            password.requestFocus();
                        }
                        String userPasswd = data.getStringExtra("pass");
                        if (!userPasswd.isEmpty()) {
                            password.setText(userPasswd);
                        }
                        if (!name.isEmpty() && !userPasswd.isEmpty()) {
                            mUsername = name;
                            mPassword = userPasswd;
                            AWSHelper.getPool().getUser(mUsername).getSessionInBackground(authenticationHandler);
                        }
                    }
                }
                break;
            case 2:
                //Confirm register user
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    if (!name.isEmpty()) {
                        username.setText(name);
                        password.setText("");
                        password.requestFocus();
                    }
                }
                break;
            case 3:
                //Forgot password
                if (resultCode == RESULT_OK) {
                    String newPass = data.getStringExtra("newPass");
                    String code = data.getStringExtra("code");
                    if (newPass != null && code != null) {
                        if (!newPass.isEmpty() && !code.isEmpty()) {
                            mForgotPasswordContinuation.setPassword(newPass);
                            mForgotPasswordContinuation.setVerificationCode(code);
                            mForgotPasswordContinuation.continueTask();
                        }
                    }
                }
                break;
            case 4:
                //User
                if (resultCode == RESULT_OK) {
                    clearInput();
                    String name = data.getStringExtra("TODO");
                    if (name != null) {
                        if (!name.isEmpty()) {
                            name.equals("exit");
                            onBackPressed();
                        }
                    }
                }
                break;
        }
    }

    /*Authentication Handler: This is used to verify if users are registered in Cognito, This is also the method
    * you can add your MFA in as well as the Authentication Challenge*/
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            /*If user logs in successfully then you want to set the currentUser. This way if you close the app the data is stored at they
            * can continue to the app without having to sign in again*/
            AWSHelper.setCurrSession(userSession);
            /*From here if the user successfully signs in you can launch the user to the main screen*/
            LaunchMainScreen();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            getUserAuthentication(authenticationContinuation, userId);

        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            /*You can leave this blank if you are not using it*/
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            /*You can leave this blank if you are not using it*/
        }

        @Override
        public void onFailure(Exception exception) {
            /*If the user fails to login for any reason you can show a Alert Dialog and the exception Cognito reports back
            * this way the user knows exactly what is wrong and can try to correct it*/

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Error");
            try {
                String error = AWSHelper.formatException(exception);
                alertDialog.setMessage(error);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } catch (Exception e) {

            }

        }
    };

    /*This method finalizes the Authentication process*/
    private void getUserAuthentication(AuthenticationContinuation continuation, String username){
        if(username != null){
            this.mUsername = username;
            AWSHelper.setUser(username);
        }
        if(this.mPassword == null){
            this.username.setText(username);
            mPassword = password.getText().toString();
            if(mPassword == null){
                return;
            }
            if(mPassword.length() < 1){
                return;
            }
        }
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(this.mUsername, this.mPassword, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    /*This method is used to move to the next screen*/
    private void LaunchMainScreen(){
        /*In this method we can either choose to use fragments and replace the current view with a new fragment or
        * we can use and intent to launch a new activity*/

        /*Open new activity*/
        Intent intent = new Intent(this, HomeScreen.class);
        startActivity(intent);

        /*Open new fragment, We will use another method, that will handle the fragment transaction,
        * Line is commented out to show how it is done, but we will continue with activity*/
//        displaySelectedScreen();
    }

    /*This method will handle the fragment and replacing the current screen with the fragment.
    * You can use a switch case as well for different screens depending on your needs*/
//    private void displaySelectedScreen(){
//        Fragment fragment = new HomeScreenFragment();
//        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
//        fragmentTransaction.replace(R.id.main_frame, fragment);
//        fragmentTransaction.commit();
//    }
}
