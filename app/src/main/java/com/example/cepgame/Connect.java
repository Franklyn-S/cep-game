package com.example.cepgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Connect extends AppCompatActivity {

    TextView tvStatus;
    EditText serverIp;
    EditText clientCEP;
    Button connect;
    Socket clientSocket;
    DataOutputStream socketOutput;
    BufferedReader socketEntrada;
    DataInputStream socketInput;
    String character= "";
    String serverCep = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        tvStatus = findViewById(R.id.status);
        serverIp = findViewById(R.id.editTextIP);
        clientCEP = findViewById(R.id.editTextCepClient);
        connect = findViewById(R.id.btConnect);
    }

    public void connect(View v) {
        final String ip = serverIp.getText().toString();
        final String cepClient = clientCEP.getText().toString();
        Log.v("PDM", ip);
        tvStatus.setText("Conectando em "+ip+":9090");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket(ip, 9090);
                    tvStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Conectado com "+ip+":9090");
                        }
                    });
                    connect.post(new Runnable() {
                        @Override
                        public void run() {
                            connect.setEnabled(false);
                        }
                    });
                    socketOutput = new DataOutputStream(clientSocket.getOutputStream());
                    socketInput = new DataInputStream(clientSocket.getInputStream());
                    String result = "";
                    Intent intent = new Intent(Connect.this, GameScreen.class);
                    Bundle bundle = new Bundle();
                    while(!result.equals("close")) {
                        Log.v("PDM", "recebido no client: " + result);
                        socketOutput.writeUTF(cepClient);
                        socketOutput.flush();
                        result = socketInput.readUTF();

                        if (result.equals("Eloi")) {
                            character="Morlock";
                            result="";
                        } else if (result.equals("Morlock")) {
                            character="Eloi";
                            result="";
                        }
                        if (!result.equals("") && result.length() == 8) {
                            serverCep = result;
                            Log.v("PDM", "Cep recebido no cliente: " +serverCep);
                            bundle.putString("cep", serverCep);
                            bundle.putString("ip", ip);
                            bundle.putBoolean("server", false);
                            intent.putExtras(bundle);
                        }
                        if (!character.equals("") && !serverCep.equals("")){
                            Log.v("PDM", "Manda fechar");
                            socketOutput.writeUTF("close");
                            socketOutput.flush();
                            result = "close";
                        }
                    }
                    desconectar();
                    startActivity(intent);
                    finish();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void desconectar() {
        try {
            if(socketOutput!=null) {
                socketOutput.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}