package cy.crbook;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import cy.common.persist.RemotePersistManager;
import cy.crbook.util.StringResultPostProcess;
import cy.crbook.util.SignupPostProcess;
import cy.readall.R;

public class LoginActivity extends Activity implements StringResultPostProcess, SignupPostProcess{
	
	private static final String TAG = "LoginActivity";
	
	EditText userNameET=null;
	EditText passwordET=null;
	EditText repasswordET=null;
	CRApplication myApp;
	
	private void showTopToast(String message){
		Toast t = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
		t.setGravity(Gravity.TOP, 0, 0);
		t.show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApp = (CRApplication) this.getApplication();
		setContentView(R.layout.login);
		
		userNameET = (EditText) findViewById(R.id.userName);
		passwordET = (EditText) findViewById(R.id.password);
		repasswordET = (EditText)findViewById(R.id.repassword);
		
		
		Button signUpBtn = (Button)findViewById(R.id.signUp);
		signUpBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (userNameET.getText().toString().equals("")){
					showTopToast("User Name is empty!");
				}else if (passwordET.getText().toString().equals(repasswordET.getText().toString())){
					(new AsyncSignup(userNameET.getText().toString(), 
							passwordET.getText().toString(), LoginActivity.this, myApp)).execute();
				}else{
					showTopToast("Password not match!");
				}
			}
		});
		
		Button loginBtn = (Button)findViewById(R.id.login);
		loginBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				(new AsyncLogon(myApp.getDeviceId(), userNameET.getText().toString(), 
						passwordET.getText().toString(), LoginActivity.this, myApp)).execute();
			}
		});
		
		Button cancelBtn = (Button)findViewById(R.id.cancel);
		cancelBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				LoginActivity.this.finish();
			}
		});
	}

	@Override
	public void postProcess(String loginResult) {
		if (loginResult==null){
			//no device id
			showTopToast("Device id not found. press cancel.");
		}else if (RemotePersistManager.LOGIN_FAILED.equals(loginResult)){
			//login failed
			showTopToast("Login failed, check your user/password.");
		}else{
			//login succeeded
			showTopToast("Login succeeded.");
			finish();
		}
	}

	@Override
	public void signupPostProcess(String signupResult) {
		if (RemotePersistManager.SIGN_UP_SUCCEED.equals(signupResult)){
			//sign up succeed
			showTopToast("sign up succeed, login...");
			new AsyncLogon(myApp.getDeviceId(), userNameET.getText().toString(), 
					passwordET.getText().toString(), LoginActivity.this, myApp).execute();
		}else{
			showTopToast(signupResult);
		}
	}
}
