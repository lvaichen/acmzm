package com.lincanbin.carbonforum;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lincanbin.carbonforum.config.APIAddress;
import com.lincanbin.carbonforum.tools.VerificationCode;
import com.lincanbin.carbonforum.util.HttpUtil;
import com.lincanbin.carbonforum.util.JSONUtil;
import com.lincanbin.carbonforum.util.MD5Util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private Toolbar mToolbar;
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private EditText mVerificationCodeView;
    private ImageView mVerificationCodeImageView;
    private SharedPreferences mSharedPreferences;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            // Title
            mToolbar.setTitle(R.string.login);
            setSupportActionBar(mToolbar);
            //Your toolbar is now an action bar and you can use it like you always do, for example:
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Set up the login form.
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);

        mVerificationCodeView = (EditText) findViewById(R.id.verification_code);
        mVerificationCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mVerificationCodeImageView = (ImageView)  findViewById(R.id.verification_code_img);
        mVerificationCodeImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                refreshVerificationCode();
            }
        });
        refreshVerificationCode();
        Button mUsernameSignInButton = (Button) findViewById(R.id.username_sign_in_button);
        mUsernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mSharedPreferences = (SharedPreferences) this.getSharedPreferences("UserInfo", Activity.MODE_PRIVATE);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void refreshVerificationCode(){
        //接口回调的方法，完成验证码的异步读取与显示
        VerificationCode verificationCodeImage = new VerificationCode(this);
        verificationCodeImage.loadImage(new VerificationCode.ImageCallBack() {
            @Override
            public void getDrawable(Drawable drawable) {
                // TODO Auto-generated method stub
                mVerificationCodeImageView.setImageDrawable(drawable);
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mVerificationCodeView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String verification_code = mVerificationCodeView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        /*
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
         */
        }
        // Check for a valid username address.
        if (TextUtils.isEmpty(verification_code)) {
            mVerificationCodeView.setError(getString(R.string.error_field_required));
            focusView = mVerificationCodeView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password, verification_code);
            mAuthTask.execute((Void) null);
        }
    }
    /*
    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return username.contains("@");
    }
    */

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void,JSONObject> {

        private final Map<String, String> parameter = new HashMap<>();

        UserLoginTask(String username, String password, String verification_code) {
            parameter.put("UserName", username);
            parameter.put("Password", MD5Util.md5(password));
            parameter.put("VerifyCode", verification_code);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            return HttpUtil.postRequest(LoginActivity.this, APIAddress.LOGIN_URL,parameter, true, false);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mAuthTask = null;
            showProgress(false);
            if(result !=null) {
                try {
                    Log.v("JSON", result.toString());
                    if (result.getInt("Status") == 1) {
                        Log.v("JSON", result.toString());

                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putString("UserID", result.getString("UserID"));
                        editor.putString("UserExpirationTime", result.getString("UserExpirationTime"));
                        editor.putString("UserCode", result.getString("UserCode"));

                        JSONObject userInfo =  JSONUtil.json2Object(result.getString("UserInfo"));
                        if(userInfo!=null){
                            editor.putString("UserName", userInfo.getString("UserName"));
                            editor.putString("UserRoleID", userInfo.getString("UserRoleID"));
                            editor.putString("UserMail", userInfo.getString("UserMail"));
                            editor.putString("UserIntro", userInfo.getString("UserIntro"));
                        }
                        editor.apply();
                        //发送广播刷新
                        Intent intent = new Intent();
                        intent.setAction("action.refreshDrawer");
                        sendBroadcast(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, result.getString("ErrorMessage"), Toast.LENGTH_SHORT).show();
                        refreshVerificationCode();
                        //TODO: 根据ErrorCode判断Focus的目标
                        mPasswordView.setError(result.getString("ErrorMessage"));
                        mPasswordView.requestFocus();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(LoginActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}