package fr.iutlens.dubois.list

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.MessageBuilder
import org.jivesoftware.smack.roster.RosterEntry
import org.jxmpp.jid.EntityBareJid


class MessageModel() : ViewModel(), IncomingChatMessageListener, OutgoingChatMessageListener {

    val selection = MutableLiveData<RosterEntry>()
    private var _chat : Chat? = null

    val chat : Chat? get(){
        if (_chat == null) _chat = selection.value?.let{SmackStore.chatManager?.chatWith(it.jid.asEntityBareJidIfPossible())}
        return _chat
    }

    fun updateConnection() {
        _chat = null
        SmackStore.chatManager?.addIncomingListener(this)
        SmackStore.chatManager?.addOutgoingListener(this)
    }


    fun allMessagesWith(jid : String) : LiveData<List<fr.iutlens.dubois.list.Message> >? {
        return  AppDatabase.getDatabase()?.messageDao()?.getAll(jid)?.asLiveData()
    }


    fun insert(message: Message) =  viewModelScope.launch {
        AppDatabase.getDatabase()?.messageDao()?.insertAll(
              fr.iutlens.dubois.list.Message.create(message)
        )
    }

    fun insert(message: fr.iutlens.dubois.list.Message)  = viewModelScope.launch {
        AppDatabase.getDatabase()?.messageDao()?.insertAll(message)
    }
/*
    fun delete(element: Element) = viewModelScope.launch {
        AppDatabase.getDatabase()?.elementDao()?.delete(element)
    }

    fun allElements(): LiveData<List<Element>>? {
        return AppDatabase.getDatabase()?.elementDao()?.getAll()?.asLiveData()
    }

*/

    fun send(msg: String): Boolean {
        val currentChat : Chat = chat ?: return false
        try {
            currentChat.send(msg)
            return true
        } catch (e:Exception){
            Log.d("chat","Error sending message")
        }
        return false
    }

    override fun newIncomingMessage(from: EntityBareJid?, message: Message?, chat: Chat?) {
        Log.d("Incoming",message.toString())
        if (message != null) {
            insert(message)
        }
    }

    override fun newOutgoingMessage(to: EntityBareJid?, messageBuilder: MessageBuilder?, chat: Chat?) {
        if (messageBuilder == null) return
        Log.d("Outgoing",messageBuilder.toString())

        val jid = SmackStore.jid
        if (jid == null || to == null) return
        insert(fr.iutlens.dubois.list.Message(
            messageBuilder.stanzaId,
            jid,
            to.asEntityBareJidString(),
            messageBuilder.body
        ))
    }


}