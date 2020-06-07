package com.example.kotlineapp

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.NullPointerException
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        photo_button_register.setOnClickListener {
            val photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 0)
        }

        register_button_register.setOnClickListener {
            registerUser()
        }

        already_have_an_account_button_login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    var photoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode == Activity.RESULT_OK && data != null){
            photoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
            photo_image_register.setImageBitmap(bitmap)
            photo_button_register.alpha = 0f
//            val bitmapDrawable = BitmapDrawable(bitmap)
//            photo_button_register.setBackgroundDrawable(bitmapDrawable)
//            photo_button_register.setText("")
        }
    }

    private fun uploadImageToStorage(){
        if(photoUri == null) return

        val filename = UUID.randomUUID().toString()
        val reference =  storage.getReference("/images/$filename")

        reference.putFile(photoUri!!).addOnSuccessListener {
            reference.downloadUrl.addOnSuccessListener {
                addUserToDatabase(it.toString())
            }
        }
    }

    private fun addUserToDatabase(profilePicUrl: String){
        val uid = auth.uid ?: ""
        val reference = database.getReference("/users/$uid")

        val user = User(uid, username_edit_register.text.toString(), profilePicUrl)

        reference.setValue(user).addOnSuccessListener {
            Toast.makeText(this, "Database Done", Toast.LENGTH_SHORT).show()

        }
    }

    private fun registerUser(){
        val username = username_edit_register.text.toString()
        val email = email_edit_register.text.toString()
        val password = password_edit_register.text.toString()
        val confirmPassword = repassword_edit_register.text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(this, "Input fields are empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword){
            Toast.makeText(this, "Password doesn't match", Toast.LENGTH_SHORT).show()
            password_edit_register.setText("")
            repassword_edit_register.setText("")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    uploadImageToStorage()

                    Toast.makeText(baseContext, "Sign up successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LatestMessagesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                else {
                    Toast.makeText(baseContext, "Sign up failed", Toast.LENGTH_SHORT).show()
                }

            }
    }

}

@Parcelize
class User(val uid: String,val username: String, val profilePicUrl: String): Parcelable{
    constructor(): this("", "", "")
}