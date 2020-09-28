package com.example.cepgame;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cepgame.model.CEP;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class GameScreen extends AppCompatActivity {

    TextView tvCep;
    EditText edCep;
    TextView logradouroValue;
    TextView cityValue;
    TextView statusValue;
    TextView pontuacaoValue;
    TextView tvResult;
    TextView tvCharacter;
    int pontuacao = 1000;
    TextView tentativasValue;
    int tentativas = 0;
    Button btTestCEP;
    String cep = "";
    String cep1 = "";
    String cep2 = "";
    String character = "";
    String ip = "";
    String message = "";
    Boolean isServer = false;
    boolean gameOver = false;
    //server
    ServerSocket welcomeSocket;
    DataOutputStream socketOutput;
    DataInputStream fromClient;
    //client
    Socket clientSocket;
    DataInputStream socketInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);

        tvResult = findViewById(R.id.tvResult);
        tvCharacter = findViewById(R.id.tvCharacter);
        tvCep = findViewById(R.id.tvCep);
        edCep = findViewById(R.id.editTextCep);
        btTestCEP = findViewById(R.id.btTestCEP);
        logradouroValue = findViewById(R.id.logradouroValue);
        cityValue = findViewById(R.id.cityValue);
        statusValue = findViewById(R.id.statusValue);
        pontuacaoValue = findViewById(R.id.pontuacaoValue);
        tentativasValue = findViewById(R.id.tentativasValue);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            cep = b.getString("cep");
            character = b.getString("character", "");
            tvCharacter.setText(character);
            ip = b.getString("ip");
            isServer = b.getBoolean("server", false);
            if(isServer){
                createServer();
            } else {
                connectClient();
            }
        }
        Log.v("PDM", "cep: "+cep);
        if (cep.length() == 8){
            cep1 = cep.substring(0,2);
            cep2 = cep.substring(3);
        }
        tvCep.setText(cep2);
    }
    @SuppressLint("SetTextI18n")
    public void testCep(View v){
        String testedCep = edCep.getText().toString().concat(cep2);
        Log.v("PDM", "Cep testado: "+testedCep);
        try {
            CEP retorno = new HttpService(testedCep).execute().get();
            if (retorno != null) {
                logradouroValue.setText(retorno.getLogradouro());
                cityValue.setText(retorno.getCidade());
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            Toast t = Toast.makeText(GameScreen.this, "CEP não", Toast.LENGTH_LONG);
            t.show();
            e.printStackTrace();
        }
        if (edCep.getText().toString().equals(cep1)){
            Toast t = Toast.makeText(GameScreen.this, "Você acertou!", Toast.LENGTH_LONG);
            t.show();
            gameOver = true;
        } else {
            tentativas++;
            tentativasValue.setText(Integer.toString(tentativas));
            pontuacao--;
            pontuacaoValue.setText(Integer.toString(pontuacao));
            if (Integer.parseInt(edCep.getText().toString()) > Integer.parseInt(cep1)){
                statusValue.setText("Maior");
            } else if (Integer.parseInt(edCep.getText().toString()) == Integer.parseInt(cep1)){
                statusValue.setText("Igual");
            } else {
                statusValue.setText("Menor");
            }
        }
    }

    public void createServer(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = "";
                try {
                    Log.v("PDM", "Ligando o Server");
                    welcomeSocket = new ServerSocket(9090);
                    Socket connectionSocket = welcomeSocket.accept();
                    Log.v("PDM", "Nova conexão");

                    //Instanciando os canais de stream
                    fromClient = new DataInputStream(connectionSocket.getInputStream());
                    socketOutput = new DataOutputStream(connectionSocket.getOutputStream());
                    while (!result.equals("endgame") || gameOver) {
                        result = fromClient.readUTF();

                        if (!result.equals("") && !result.equals("endgame")){
                            message = result;
                        }
                        if (result.equals("endgame")) {
                            //O outro ganhou
                            tvResult.setText(message);
                            tvResult.setVisibility(View.VISIBLE);
                        } else if (gameOver) {
                            //ganhei
                            tvResult.setText("Parabéns você acertou o cep!");
                            tvResult.setVisibility(View.VISIBLE);
                            socketOutput.writeUTF("O outro jogador ("+character+") ganhou com "+tentativas+", fazendo "+pontuacao+" pontos");
                            socketOutput.flush();
                            socketOutput.writeUTF("endgame");
                            socketOutput.flush();
                        }
                    }
                    desconectar();
                    Log.v("PDM", "Desconectado");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

    }
    public void connectClient(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket(ip, 9090);
                    socketOutput = new DataOutputStream(clientSocket.getOutputStream());
                    socketInput = new DataInputStream(clientSocket.getInputStream());
                    String result = "";
                    while(result != "endgame" || gameOver) {
                        result = socketInput.readUTF();
                        if (result != "" && result != "endgame"){
                            message = result;
                        }
                        if (result == "endgame") {
                            //O outro ganhou
                            tvResult.setText(message);
                            tvResult.setVisibility(View.VISIBLE);
                        } else if (gameOver) {
                            //ganhei
                            tvResult.setText("Parabéns você acertou o cep!");
                            tvResult.setVisibility(View.VISIBLE);
                            socketOutput.writeUTF("O outro jogador ("+character+") ganhou com "+tentativas+", fazendo "+pontuacao+" pontos");
                            socketOutput.flush();
                            socketOutput.writeUTF("endgame");
                            socketOutput.flush();
                        }
                    }
                    socketOutput.writeUTF("close");
                    socketOutput.flush();
                    desconectar();
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