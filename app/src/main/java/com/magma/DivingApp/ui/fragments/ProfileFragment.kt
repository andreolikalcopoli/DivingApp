package com.magma.DivingApp.ui.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.magma.DivingApp.R
import com.magma.DivingApp.databinding.FragmentProfileBinding
import com.magma.DivingApp.model.UserModel
import com.magma.DivingApp.ui.RegisterActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class ProfileFragment:Fragment(){
    private lateinit var binding: FragmentProfileBinding
    val databaseInstance: DatabaseReference = FirebaseDatabase.getInstance().reference.child("users")
    private lateinit var attachedContext: Context
    private var selectedImage = ""
    private var  userID = ""
    val user = FirebaseAuth.getInstance().currentUser

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImage = uri.toString()
            uri?.let { binding.imgPRofile.setImageURI(uri) }
            uploadProfileImageFromUri(attachedContext,uri!!,userID)
            databaseInstance.child(userID).child("data").child("image").setValue(selectedImage)

        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        attachedContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater)
        init()
        return binding.root
    }



    private fun init() {
        binding.etEmail.isEnabled = false
        binding.etName.isFocusable = false
        binding.etId.isEnabled = false
        binding.etName.setOnClickListener {
            showAlertDialogWithTwoButtons()
        }
        binding.imgPRofile.setOnClickListener {
            selectImageFromGalleryResult.launch("image/*")
        }
        val sharedPreferences = attachedContext?.getSharedPreferences("pref", Context.MODE_PRIVATE)
        userID = sharedPreferences?.getString("userID","")!!
        databaseInstance.child(sharedPreferences?.getString("userID","")!!).child("data").get().addOnCompleteListener {

            val user =   it.result.getValue(UserModel::class.java)
            binding.etEmail.setText(user?.email)
            binding.etId.setText(user?.uid)
            binding.etName.setText(user?.name)
            binding.topText.text = user?.name
            val n = user?.name
            binding.botText.text = "@$n"
            Glide.with(attachedContext).load(user?.image).placeholder(R.drawable.devon).into(binding.imgPRofile)

        }
        binding.delLabel.setOnClickListener {
            shodeleteDialog()
        }
    }
    private fun shodeleteDialog() {
        AlertDialog.Builder(attachedContext)
            .setTitle("Are you Sure?")
            .setMessage("This will Delete Your Account permanently")
            .setPositiveButton("Delete") { _, _ ->
                user?.delete()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val sharedPreferences = attachedContext?.getSharedPreferences("pref", Context.MODE_PRIVATE)
                            val intent = Intent(attachedContext,RegisterActivity::class.java)

                            databaseInstance.child("users").child(userID).removeValue()
                            val editor = sharedPreferences?.edit()
                            editor?.putString("userID", "")
                            editor?.apply()

                            FirebaseAuth.getInstance().signOut()

                            startActivity(intent)
                        } else {
                            // Handle the error
                            val exception = task.exception
                            if (exception != null) {
                                // Handle the exception
                                // You can display an error message to the user or log it for debugging
                            }
                        }
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun showAlertDialogWithTwoButtons() {
        val sharedPreferences = attachedContext?.getSharedPreferences("pref", Context.MODE_PRIVATE)

        val inputEditTextField = EditText(attachedContext)
        val dialog = AlertDialog.Builder(attachedContext)
            .setTitle("Enter Your Name")
            .setMessage("this will be saved as your username")
            .setView(inputEditTextField)
            .setPositiveButton("OK") { _, _ ->
                val editTextInput = inputEditTextField .text.toString()

                val paddingValue = resources.getDimensionPixelSize(com.mapbox.search.ui.R.dimen.material_filled_edittext_font_1_3_padding_bottom) // You can define your desired padding value in dimensions.xml
                inputEditTextField.setPadding(paddingValue, paddingValue, paddingValue, paddingValue)

                databaseInstance.child(sharedPreferences?.getString("userID","")!!).child("data").child("name").setValue(editTextInput).let {
                    activity?.recreate()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    fun uploadProfileImageFromUri(context: Context, imageUri: Uri, user: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("profile_images/$user.jpg")
        val imageStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val imageBitmap: Bitmap = BitmapFactory.decodeStream(imageStream)

        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val imageData = baos.toByteArray()

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()


            }


}