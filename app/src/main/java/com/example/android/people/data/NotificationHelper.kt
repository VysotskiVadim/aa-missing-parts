/*
 * Copyright (C) 2020 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.people.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.example.android.people.MainActivity
import com.example.android.people.R
import com.example.android.people.ReplyReceiver

/**
 * Handles all operations related to [Notification].
 */
class NotificationHelper(private val context: Context) {

    companion object {
        /**
         * The notification channel for messages. This is used for showing Bubbles.
         */
        private const val CHANNEL_NEW_MESSAGES = "new_messages"

        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2
    }

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: throw IllegalStateException()

    fun setUpNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_NEW_MESSAGES) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_NEW_MESSAGES,
                        context.getString(R.string.channel_new_messages),
                        // The importance must be IMPORTANCE_HIGH to show Bubbles.
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.channel_new_messages_description)
                    }
                )
            }
        }
    }

    @WorkerThread
    fun showNotification(chat: Chat, fromUser: Boolean) {
        val icon = IconCompat.createWithAdaptiveBitmapContentUri(chat.contact.iconUri)
        val contentUri = "https://android.example.com/chat/${chat.contact.id}".toUri()

        val builder = NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)

            // The user can turn off the bubble in system settings. In that case, this notification
            // is shown as a normal notification instead of a bubble. Make sure that this
            // notification works as a normal notification as well.
            .setContentTitle(chat.contact.name)
            .setSmallIcon(R.drawable.ic_message)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(chat.contact.shortcutId)
            .setShowWhen(true)
            // The content Intent is used when the user clicks on the "Open Content" icon button on
            // the expanded bubble, as well as when the fall-back notification is clicked.
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CONTENT,
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            // Direct Reply
            .addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_send),
                        context.getString(R.string.label_reply),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_CONTENT,
                            Intent(context, ReplyReceiver::class.java).setData(contentUri),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .addRemoteInput(
                        RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY)
                            .setLabel(context.getString(R.string.hint_input))
                            .build()
                    )
                    .setAllowGeneratedReplies(true)
                    .build()
            )
            // Let's add some more content to the notification in case it falls back to a normal
            // notification.
//            .setStyle(
//                NotificationCompat.MessagingStyle(user)
//                    .apply {
//                        val lastId = chat.messages.last().id
//                        for (message in chat.messages) {
//                            val m = Notification.MessagingStyle.Message(
//                                message.text,
//                                message.timestamp,
//                                if (message.isIncoming) person else null
//                            ).apply {
//                                if (message.photoUri != null) {
//                                    setData(message.photoMimeType, message.photoUri)
//                                }
//                            }
//                            if (message.id < lastId) {
//                                addHistoricMessage(m)
//                            } else {
//                                addMessage(m)
//                            }
//                        }
//                    }
//                    .setGroupConversation(false)
//            )
            .setWhen(chat.messages.last().timestamp)

        notificationManager.notify(chat.contact.id.toInt(), builder.build())
    }

    fun dismissNotification(id: Long) {
        notificationManager.cancel(id.toInt())
    }
}
