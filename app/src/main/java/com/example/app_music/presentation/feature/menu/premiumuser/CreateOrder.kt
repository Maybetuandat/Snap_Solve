package com.example.app_music.presentation.feature.menu.premiumuser

import android.util.Log
import com.example.app_music.data.remote.api.HttpProvider
import com.example.app_music.domain.utils.AppInfo
import com.example.app_music.domain.utils.Helpers
import org.json.JSONObject
import java.util.Date
import okhttp3.FormBody
import okhttp3.RequestBody

class CreateOrder {
    private inner class CreateOrderData(amount: String, isForPremium: Boolean = false) {

        val appId: String // do zalopay cung cap
        val appUser: String  // Ten app cua ca nhan
        val appTime: String  // mui gio hien tai
        val amount: String   // gia tien thanh toan -> doi voi premium user mac dinh la 98000
        val appTransId: String  // appTransId duoc lay tu lop Helper cung cap san tu thu vien cua zalopay -> id dinh danh cho cho don hang
        val embedData: String  // thong tin ve chien luoc thanh toan -> mac dinh la rong.
        val items: String   // danh sach san pham nguoi dung dang mua
        val bankCode: String  // ma ngan hang -> khi su dung zlpay thi se la zalopay
        val description: String   // mo ta don hang
        val mac: String  // ky de zalo thuc hien xac thuc

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

        Log.d("CreateOrder", "=== ORDER CREATION DEBUG ===")
        Log.d("CreateOrder", "App ID: ${input.appId}")
        Log.d("CreateOrder", "App Trans ID: ${input.appTransId}")
        Log.d("CreateOrder", "Amount: ${input.amount}")
        Log.d("CreateOrder", "MAC: ${input.mac}")
        Log.d("CreateOrder", "Embed Data: ${input.embedData}")
        Log.d("CreateOrder", "Items: ${input.items}")
        Log.d("CreateOrder", "Description: ${input.description}")
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

        val response =  HttpProvider.sendPost(AppInfo.URL_CREATE_ORDER, formBody)   // tao mot formbody va gui request den phia backend server cua zalopay
      //  Log.d("CreateOrder", "Response: ${response.toString(2)}")
        return response
    }
}