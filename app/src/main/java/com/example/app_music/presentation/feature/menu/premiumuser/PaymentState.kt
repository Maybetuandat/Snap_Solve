    package com.example.app_music.presentation.feature.menu.premiumuser

    sealed class PaymentState {
        object Idle : PaymentState()  // chua vao tien trinh thanh toan
        object Processing : PaymentState() // dang xu ly thanh toan -> goi API de tao order
        data class Success(val transactionId: String, val transToken: String, val amount: String) : PaymentState()   // sucess se tra ve id giao dich, token ordercreate , va so tien thanh toan
        data class Error(val errorMessage: String) : PaymentState()  // trang thai loi
        object Cancelled : PaymentState()  // trang thai huy thanh toan
        data class OrderCreated(val zpTransToken: String) : PaymentState()  // da tao thanh cong va co zpTransToken -> token dai dien cho OrderCreate
    }