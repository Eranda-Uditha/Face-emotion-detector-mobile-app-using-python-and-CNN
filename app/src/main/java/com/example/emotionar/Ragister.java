package com.example.emotionar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.regex.Pattern;

public class Ragister extends AppCompatActivity {

    EditText email, password, repassword;
    Button signup;
    DBHelper DB;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ragister);

        email = findViewById(R.id.email); // assuming you have an EditText with id email
        password = findViewById(R.id.password);
        repassword = findViewById(R.id.repassword);
        signup = findViewById(R.id.signup);
        DB = new DBHelper(this);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userEmail = email.getText().toString().trim();
                String pass = password.getText().toString();
                String repass = repassword.getText().toString();

                if (!isValidEmail(userEmail)) {
                    Toast.makeText(Ragister.this, "Invalid Email Address", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(pass) || TextUtils.isEmpty(repass)) {
                    Toast.makeText(Ragister.this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show();
                } else if (!isPasswordValid(pass)) {
                    Toast.makeText(Ragister.this, "Password should be at least 8 characters long", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(repass)) {
                    Toast.makeText(Ragister.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else {
                    // All validations passed, proceed with registration
                    Boolean checkuser = DB.checkusername(userEmail); // Assuming your DBHelper handles email as username
                    if (!checkuser) {
                        Boolean insert = DB.insertData(userEmail, pass);
                        if (insert) {
                            Toast.makeText(Ragister.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), Login.class);  // Redirect to login activity
                            startActivity(intent);
                        } else {
                            Toast.makeText(Ragister.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Ragister.this, "User already Exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    // Function to validate email using Android's Patterns class
    private boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    // Function to validate password (example: at least 8 characters long)
    private boolean isPasswordValid(String password) {
        return password.length() >= 8;
    }
}
