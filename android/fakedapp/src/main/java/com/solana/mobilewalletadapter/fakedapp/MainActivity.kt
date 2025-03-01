/*
 * Copyright (c) 2022 Solana Mobile Inc.
 */

package com.solana.mobilewalletadapter.fakedapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.GuardedBy
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.solana.mobilewalletadapter.fakedapp.databinding.ActivityMainBinding
import com.solana.mobilewalletadapter.fakedapp.usecase.MemoTransactionVersion
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var viewBinding: ActivityMainBinding
    private val activityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            intentSender.onActivityComplete()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { uiState ->
                    uiState.hasAuthToken.let { isAuthorized ->
                        viewBinding.btnReauthorize.isEnabled = isAuthorized
                        viewBinding.btnDeauthorize.isEnabled = isAuthorized
                        viewBinding.btnRequestAirdrop.isEnabled = isAuthorized
                        viewBinding.btnSignTxnX1.isEnabled = isAuthorized
                        viewBinding.btnSignTxnX3.isEnabled = isAuthorized
                        viewBinding.btnSignTxnX20.isEnabled = isAuthorized
                        viewBinding.btnSignMsgX1.isEnabled = isAuthorized
                        viewBinding.btnSignMsgX3.isEnabled = isAuthorized
                        viewBinding.btnSignMsgX20.isEnabled = isAuthorized
                        viewBinding.btnSignAndSendTxnX1.isEnabled = isAuthorized
                        viewBinding.btnSignAndSendTxnX3.isEnabled = isAuthorized
                        viewBinding.btnSignAndSendTxnX20.isEnabled = isAuthorized
                        viewBinding.cbHasAuthToken.isChecked = isAuthorized
                    }

                    viewModel.supportedTxnVersions.indexOf(uiState.txnVersion).let { spinnerPos ->
                        if (spinnerPos > 0) viewBinding.spinnerTxnVer.setSelection(spinnerPos)
                    }

                    viewBinding.tvAccountName.text =
                        uiState.accountLabel ?: getString(R.string.string_no_account_name)
                    viewBinding.tvWalletUriPrefix.text =
                        uiState.walletUriBase?.toString()
                            ?: getString(R.string.string_no_wallet_uri_prefix)

                    if (uiState.messages.isNotEmpty()) {
                        val message = uiState.messages.first()
                        Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_SHORT)
                            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    viewModel.messageShown()
                                }
                            }).show()
                    }
                }
            }
        }

        viewBinding.btnGetCapabilities.setOnClickListener {
            viewModel.getCapabilities(intentSender)
        }

        viewBinding.btnAuthorize.setOnClickListener {
            viewModel.authorize(intentSender)
        }

        viewBinding.btnReauthorize.setOnClickListener {
            viewModel.reauthorize(intentSender)
        }

        viewBinding.btnDeauthorize.setOnClickListener {
            viewModel.deauthorize(intentSender)
        }

        viewBinding.btnRequestAirdrop.setOnClickListener {
            viewModel.requestAirdrop()
        }

        viewBinding.btnSignTxnX1.setOnClickListener {
            viewModel.signTransactions(intentSender, 1)
        }

        viewBinding.btnSignTxnX3.setOnClickListener {
            viewModel.signTransactions(intentSender, 3)
        }

        viewBinding.btnSignTxnX20.setOnClickListener {
            viewModel.signTransactions(intentSender, 20)
        }

        viewBinding.btnAuthorizeSign.setOnClickListener {
            viewModel.authorizeAndSignTransactions(intentSender)
        }

        viewBinding.spinnerTxnVer.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item,
                // mapping from view model txn version to localized UI string
                viewModel.supportedTxnVersions.map { txnVersion ->
                    getString(when (txnVersion) {
                        MemoTransactionVersion.Legacy -> R.string.string_txn_version_legacy
                        MemoTransactionVersion.V0 -> R.string.string_txn_version_v0
                    })
                }
            )

        viewBinding.spinnerTxnVer.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, selected: View?,
                                            index: Int, id: Long) {
                    viewModel.setTransactionVersion(viewModel.supportedTxnVersions[index])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // nothing to do
                }
            }

        viewBinding.btnSignMsgX1.setOnClickListener {
            viewModel.signMessages(intentSender, 1)
        }

        viewBinding.btnSignMsgX3.setOnClickListener {
            viewModel.signMessages(intentSender, 3)
        }

        viewBinding.btnSignMsgX20.setOnClickListener {
            viewModel.signMessages(intentSender, 20)
        }

        viewBinding.btnSignAndSendTxnX1.setOnClickListener {
            viewModel.signAndSendTransactions(intentSender, 1)
        }

        viewBinding.btnSignAndSendTxnX3.setOnClickListener {
            viewModel.signAndSendTransactions(intentSender, 3)
        }

        viewBinding.btnSignAndSendTxnX20.setOnClickListener {
            viewModel.signAndSendTransactions(intentSender, 20)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkIsWalletEndpointAvailable()
    }

    private val intentSender = object : MainViewModel.StartActivityForResultSender {
        @GuardedBy("this")
        private var callback: (() -> Unit)? = null

        override fun startActivityForResult(
            intent: Intent,
            onActivityCompleteCallback: () -> Unit
        ) {
            synchronized(this) {
                check(callback == null) { "Received an activity start request while another is pending" }
                callback = onActivityCompleteCallback
            }
            activityResultLauncher.launch(intent)
        }

        fun onActivityComplete() {
            synchronized(this) {
                callback?.let { it() }
                callback = null
            }
        }
    }
}