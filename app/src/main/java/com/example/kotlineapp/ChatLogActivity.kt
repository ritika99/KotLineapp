package com.example.kotlineapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    val adapter = GroupAdapter<ViewHolder>()

    var fromUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerview_chatlog.adapter = adapter

//        supportActionBar?.title = "Chat Log"
//        val user = intent.getStringExtra(NewMessagesActivity.USER_KEY)
        fromUser = intent.getParcelableExtra<User>(NewMessagesActivity.USER_KEY)

        supportActionBar?.title = fromUser?.username

//        dummyData()

        listenForMessages()

        send_button_chatlog.setOnClickListener {
            sendMessage()
        }
    }

    private fun listenForMessages(){
        val fromId = fromUser?.uid
        val toId = auth.uid
        val reference = database.getReference("/user-messages/$toId/$fromId")
        reference.addChildEventListener(object: ChildEventListener {

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)

                if (chatMessage != null) {

                    if (chatMessage.toId == auth.uid) {
//                        var loginUser = LatestMessagesActivity.loginUser ?: return
                        adapter.add(ChatToItem(chatMessage.text))
                    }
                    else {
                        adapter.add(ChatFromItem(chatMessage.text, fromUser!!))
                    }
                }
                recyclerview_chatlog.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }

        })
    }

    private fun sendMessage() {
        val text = enter_message_edit_chatlog.text.toString()

        val user = intent.getParcelableExtra<User>(NewMessagesActivity.USER_KEY)
        val fromId = user.uid
        val toId = auth.uid

        if (toId == null) return

//        val reference = database.getReference("/messages").push()
        val reference = database.getReference("/user-messages/$toId/$fromId").push()

        val fromReference = database.getReference("/user-messages/$fromId/$toId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000)

        reference.setValue(chatMessage)
            .addOnSuccessListener {
                enter_message_edit_chatlog.setText("")
                recyclerview_chatlog.scrollToPosition(adapter.itemCount - 1)
            }

        fromReference.setValue(chatMessage)

        val lastestMessageReference = database.getReference("/latest-messages/$toId/$fromId")
        lastestMessageReference.setValue(chatMessage)

        val lastestMessageFromReference = database.getReference("/latest-messages/$fromId/$toId")
        lastestMessageFromReference.setValue(chatMessage)


    }

}

class ChatFromItem(val text: String, val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.chat_from_message_text.text = text

        val uri = user.profilePicUrl
        val profilePic = viewHolder.itemView.chat_from_profile_image
        Picasso.get().load(uri).into(profilePic)

    }
    override fun getLayout(): Int = R.layout.chat_from_row
}

class ChatToItem(val text: String): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.chat_to_message_text.text = text

//        val uri = user.profilePicUrl
//        val profilePic = viewHolder.itemView.chat_to_profile_image
//        Picasso.get().load(uri).into(profilePic)
    }
    override fun getLayout(): Int = R.layout.chat_to_row
}

class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timestamp: Long) {
    constructor() : this("", "", "", "", -1)
}


