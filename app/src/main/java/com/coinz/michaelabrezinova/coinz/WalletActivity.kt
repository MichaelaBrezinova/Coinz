package com.coinz.michaelabrezinova.coinz

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.View

import kotlinx.android.synthetic.main.activity_wallet.*
import android.support.v4.app.NavUtils



class WalletActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
       // setSupportActionBar(toolbar)

         backMapButton?.setOnClickListener {
             val intent = Intent(applicationContext, MapsActivity::class.java)
             startActivity(intent)
         }

       //supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
