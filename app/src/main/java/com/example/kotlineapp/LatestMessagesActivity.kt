
package com.example.kotlineapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messgaes.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessagesActivity : AppCompatActivity() {

    companion object{
        var loginUser: User? = null
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messgaes)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        recyclerview_latest_messages.adapter = adapter

        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessageItem

            intent.putExtra(NewMessagesActivity.USER_KEY, row.chatPartnerUser)
            startActivity(intent)

        }

        verifyLoggedInUser()
        listenForLatestMessages()
    }


    private fun verifyLoggedInUser(){
        val uid = auth.uid
        if (uid == null){
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater .inflate(R.menu.navbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.new_message_menu -> {
                val intent = Intent(this, NewMessagesActivity::class.java)
                startActivity(intent)
            }
            R.id.sign_out_menu -> {
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    val latestMessagesMap = HashMap<String, ChatMessage>()

    private fun refreshRecyclerViewLatestMessage(){
        adapter.clear()
        latestMessagesMap.values.forEach{
            adapter.add(LatestMessageItem(it))
        }
    }

    private fun listenForLatestMessages(){
        val toId = auth.uid
        val reference = database.getReference("/latest-messages/$toId")
        reference.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerViewLatestMessage()
            }
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerViewLatestMessage()
            }

            override fun onCancelled(p0: DatabaseError) {

            }
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }
            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }
}

class LatestMessageItem(val chatMessage: ChatMessage): Item<ViewHolder>() {
    var chatPartnerUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.chat_message_text_latest_messages.text = chatMessage.text
        val chatPartnerId: String
        if (chatMessage.toId == FirebaseAuth.getInstance().uid){
            chatPartnerId = chatMessage.fromId
        }
        else{
            chatPartnerId = chatMessage.toId
        }

        val reference = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
        reference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                chatPartnerUser = p0.getValue(User::class.java)
                viewHolder.itemView.username_text_latest_messages.text = chatPartnerUser?.username
                val image = viewHolder.itemView.user_image_latest_messages
                Picasso.get().load(chatPartnerUser?.profilePicUrl).into(image)
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }
    override fun getLayout(): Int = R.layout.latest_message_row
}
