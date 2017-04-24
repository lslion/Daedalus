package org.itxtech.daedalus.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.R;
import org.itxtech.daedalus.activity.MainActivity;
import org.itxtech.daedalus.service.DaedalusVpnService;
import org.itxtech.daedalus.util.DnsServer;

/**
 * Daedalus Project
 *
 * @author iTXTech
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
public class MainFragment extends Fragment {

    private View view = null;
    private MainFragmentHandler mHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);

        final Button but = (Button) view.findViewById(R.id.button_activate);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Daedalus.getInstance().isServiceActivated()) {
                    Daedalus.getInstance().deactivateService();
                } else {
                    activateService();
                }
            }
        });

        updateUserInterface();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            mHandler = (new MainFragmentHandler()).setFragment(this);
            ((MainActivity) context).setMainFragmentHandler(mHandler);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mHandler.shutdown();
        MainActivity.getInstance().setMainFragmentHandler(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        view = null;
    }

    public void activateService() {
        Intent intent = VpnService.prepare(Daedalus.getInstance());
        view.findViewById(R.id.button_activate).setVisibility(View.INVISIBLE);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, Activity.RESULT_OK, null);
        }
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result == Activity.RESULT_OK) {
            DaedalusVpnService.primaryServer = DnsServer.getDnsServerAddressById(Daedalus.getPrefs().getString("primary_server", "0"));
            DaedalusVpnService.secondaryServer = DnsServer.getDnsServerAddressById(Daedalus.getPrefs().getString("secondary_server", "1"));

            Daedalus.getInstance().startService(Daedalus.getInstance().getServiceIntent().setAction(DaedalusVpnService.ACTION_ACTIVATE));

            Button button = (Button) view.findViewById(R.id.button_activate);
            button.setText(R.string.button_text_deactivate);
            button.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    private void updateUserInterface() {
        Log.d("DMainFragment", "updateInterface");
        Button but = (Button) view.findViewById(R.id.button_activate);
        if (Daedalus.getInstance().isServiceActivated()) {
            but.setText(R.string.button_text_deactivate);
        } else {
            but.setText(R.string.button_text_activate);
        }

    }

    public static class MainFragmentHandler extends Handler {
        public static final int MSG_REFRESH = 0;

        private MainFragment fragment = null;

        MainFragmentHandler setFragment(MainFragment fragment) {
            this.fragment = fragment;
            return this;
        }

        void shutdown() {
            fragment = null;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_REFRESH:
                    ((Button) fragment.view.findViewById(R.id.button_activate)).setText(R.string.button_text_activate);
                    break;
            }
        }
    }
}