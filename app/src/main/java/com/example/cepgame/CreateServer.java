package com.example.cepgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CreateServer extends AppCompatActivity {

    TextView tvStatus;
    ServerSocket welcomeSocket;
    DataOutputStream socketOutput;
    BufferedReader socketEntrada;
    DataInputStream fromClient;
    Button createServer;
    EditText cepNumber;
    Spinner choosenCharacter;
    String ipAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_server);
        tvStatus = findViewById(R.id.tvStatus);
        createServer = findViewById(R.id.btLigarServer);
        cepNumber = findViewById(R.id.editTextCepClient);
        choosenCharacter = findViewById(R.id.spinner);
        
    }

    public void turnOnServer(View v) {
        ConnectivityManager connManager;
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();

        for(Network network:networks){
            NetworkInfo networkInfo = connManager.getNetworkInfo(network);
            if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)){
                NetworkCapabilities netProps = connManager.getNetworkCapabilities(network);
                if(netProps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    int ip = wifiManager.getConnectionInfo().getIpAddress();
                    ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                    Log.v("PDM", "Wifi - IP:"+ipAddress);
                    tvStatus.setText("Ativo em: "+ipAddress + ", esperando outro usuário...");

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startServer();
                        }
                    });
                    t.start();
                }
            }
        }
    }

    public void startServer(){
        createServer.post(new Runnable() {
            @Override
            public void run() {
                createServer.setEnabled(false);
            }
        });
        String result = "";
        try{
            welcomeSocket = new ServerSocket(9090);
            Socket connectionSocket = welcomeSocket.accept();
            Log.v("PDM", "Nova Conexão");

            fromClient = new DataInputStream(connectionSocket.getInputStream());
            socketOutput = new DataOutputStream(connectionSocket.getOutputStream());
            Intent intent = new Intent(CreateServer.this, GameScreen.class);
            Bundle bundle = new Bundle();
            while(!result.equals("close")) {
                Log.v("PDM", "Esperando cliente! " + result);
                result = fromClient.readUTF();
                socketOutput.writeUTF(choosenCharacter.getSelectedItem().toString());
                socketOutput.flush();
                socketOutput.writeUTF(cepNumber.getText().toString());
                socketOutput.flush();

                if (!result.equals("") && !result.equals("close") && result.length() == 8) {
                    // definir cep do cliente
                    Log.v("PDM", "Cep do cliente: "+result);
                    bundle.putString("cep", result);
                    bundle.putString("character", choosenCharacter.getSelectedItem().toString());
                    bundle.putString("ip", ipAddress);
                    bundle.putBoolean("server", true);
                }
                if (result.equals("close")){
                    socketOutput.writeUTF("close");
                    socketOutput.flush();
                }
            }

            desconectar();
            intent.putExtras(bundle);
            startActivity(intent);
            finish();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void desconectar(){
        try {
            if(socketOutput!=null) {
                socketOutput.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}