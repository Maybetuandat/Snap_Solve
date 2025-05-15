package com.example.app_music.domain.model





data class User(
    val id: Long? = null,
    val username: String? = null,
    val statusMessage: String? = null,
    val studentInformation: String? = null,
    val suid: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val userRank: String? = null,
    val avatarUrl: String? = null,
    val password: String? = null
) {

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        private var id: Long? = null
        private var username: String? = null
        private var statusMessage: String? = null
        private var studentInformation: String? = null
        private var suid: String? = null
        private var phoneNumber: String? = null
        private var email: String? = null
        private var userRank: String? = null
        private var avatarUrl: String? = null
        private var password: String? = null


        fun id(id: Long?): Builder {
            this.id = id
            return this
        }

        fun username(username: String?): Builder {
            this.username = username
            return this
        }

        fun statusMessage(statusMessage: String?): Builder {
            this.statusMessage = statusMessage
            return this
        }

        fun studentInformation(studentInformation: String?): Builder {
            this.studentInformation = studentInformation
            return this
        }

        fun suid(suid: String?): Builder {
            this.suid = suid
            return this
        }

        fun phoneNumber(phoneNumber: String?): Builder {
            this.phoneNumber = phoneNumber
            return this
        }

        fun email(email: String?): Builder {
            this.email = email
            return this
        }

        fun userRank(userRank: String?): Builder {
            this.userRank = userRank
            return this
        }

        fun avatarUrl(avatarUrl: String?): Builder {
            this.avatarUrl = avatarUrl
            return this
        }

        fun password(password: String?): Builder {
            this.password = password
            return this
        }


        fun build(): User {
            return User(
                id = id,
                username = username,
                statusMessage = statusMessage,
                studentInformation = studentInformation,
                suid = suid,
                phoneNumber = phoneNumber,
                email = email,
                userRank = userRank,
                avatarUrl = avatarUrl,
                password = password
            )
        }
    }
}
