package com.example.app_music.presentation.feature.menu.premiumuser

import com.example.app_music.data.remote.api.HttpProvider
import com.example.app_music.domain.utils.AppInfo
import com.example.app_music.domain.utils.Helpers
import org.json.JSONObject
import java.util.Date
import okhttp3.FormBody
import okhttp3.RequestBody

class CreateOrder {
    private inner class CreateOrderData(amount: String, isForPremium: Boolean = false) {
        // ... existing code ... {
        val appId: String
        val appUser: String
        val appTime: String
        val amount: String
        val appTransId: String
        val embedData: String
        val items: String
        val bankCode: String
        val description: String
        val mac: String

        init {
            val appTime = Date().time
            this.appId = AppInfo.APP_ID.toString()
            this.appUser = "Premium_User"
            this.appTime = appTime.toString()
            this.amount = amount
            this.appTransId = Helpers.getAppTransId()
            this.embedData = if (isForPremium) {
                """{"premium": true, "plan": "monthly"}"""
            } else {
                "{}"
            }
            this.items = if (isForPremium) {
                """[{"itemid": "premium_plan", "itemname": "SnapSolve Premium", "itemprice": $amount, "itemquantity": 1}]"""
            } else {
                "[]"
            }
            this.bankCode = "zalopayapp"
            this.description = if (isForPremium) {
                "SnapSolve Premium subscription #${this.appTransId}"
            } else {
                "Merchant pay for order #${this.appTransId}"
            }

            val inputHMac = "${this.appId}|${this.appTransId}|${this.appUser}|${this.amount}|${this.appTime}|${this.embedData}|${this.items}"
            this.mac = Helpers.getMac(AppInfo.MAC_KEY, inputHMac)
        }
    }

    @Throws(Exception::class)
    fun createOrder(amount: String, isForPremium: Boolean = false): JSONObject {
        val input = CreateOrderData(amount, isForPremium)

        val formBody: RequestBody = FormBody.Builder()
            .add("app_id", input.appId)
            .add("app_user", input.appUser)
            .add("app_time", input.appTime)
            .add("amount", input.amount)
            .add("app_trans_id", input.appTransId)
            .add("embed_data", input.embedData)
            .add("item", input.items)
            .add("bank_code", input.bankCode)
            .add("description", input.description)
            .add("mac", input.mac)
            .build()

        return HttpProvider.sendPost(AppInfo.URL_CREATE_ORDER, formBody)
    }
}