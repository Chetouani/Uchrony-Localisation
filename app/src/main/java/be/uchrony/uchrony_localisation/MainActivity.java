package be.uchrony.uchrony_localisation;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.connection.OnServiceBoundListener;
import com.kontakt.sdk.android.device.BeaconDevice;
import com.kontakt.sdk.android.device.Region;
import com.kontakt.sdk.android.factory.Filters;
import com.kontakt.sdk.android.manager.BeaconManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.os.Handler;


public class MainActivity extends ActionBarActivity {

    // Balise utilisé pour le debuguage
    private String TAG_DEBUG = "TAG_DEBUG_MainActivity";
    // identifient de la demande d'activation du blue
    // pour verifier que l'activation est ok
    private static final int CODE_ACTIVATION_BLUETOOTH = 1;
    // Permet la gestion des beacons et du scan
    private BeaconManager beaconManager;
    private HashSet<Region> listeRegion;
    // Permet de savoir si le scan est en cours ou alors arréter
    private boolean enCoursDeScan = false;
    private ArrayList<Double> distancesA = new ArrayList<>();
    private ArrayList<Double> distancesB = new ArrayList<>();
    private ArrayList<Double> distancesC = new ArrayList<>();
    private ArrayList<Double> distancesD = new ArrayList<>();
    private ArrayList<Point> zones = new ArrayList<>();
    List<Point> listePosition;
    //private PlanPiece planPiece;
    private PlanPieceQuatre planPieceQuatre;
    public TextView x,t,w,coin,z;
    ImageView drawingImageView;
    Canvas canvas;
    long i=1;
    String textMessage="";


    private double VAR1 = 0.89976;
    private double VAR2 = 7.7095;
    private static int TAILLE_LISTE = 0;
    // private Trilateration trilateration;
    private QuadriLateration quadriLateration;

    long lastUpdate=-1;
    long curtime=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initElements();
        afficherPlan(null,null);
        initBeacon();
        verificationBluetooth();
    }

    private void afficherPlan(Point position,PlanPieceQuatre.Coin coin) {
        drawingImageView = (ImageView) findViewById(R.id.img);
        drawingImageView.setImageResource(R.drawable.metting_2);
        BitmapDrawable drawable = (BitmapDrawable) drawingImageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        bitmap = Bitmap.createScaledBitmap(bitmap, 500 *2, 477*2, true);
        canvas = new Canvas(bitmap);
        drawingImageView.setImageBitmap(bitmap);


        // Desinne le Rectangle avec les barres au centre
        Paint quadrillagePiece = new Paint();
        quadrillagePiece.setColor(Color.BLUE);
        quadrillagePiece.setStrokeWidth(5);

        canvas.drawLine(0
                , 95.4f*2
                , 500*2
                , 95.4f*2, quadrillagePiece);
        canvas.drawLine(100*2
                , 0
                , 100*2
                , 477*2, quadrillagePiece);
        canvas.drawLine(0
                , 95.4f*4
                , 500*2
                , 95.4f*4, quadrillagePiece);
        canvas.drawLine(100*4
                , 0
                , 100*4
                , 477*2, quadrillagePiece);

        canvas.drawLine(0
                , 95.4f*6
                , 500*2
                , 95.4f*6, quadrillagePiece);
        canvas.drawLine(100*6
                , 0
                , 100*6
                , 477*2, quadrillagePiece);
        canvas.drawLine(0
                , 95.4f*8
                , 500*2
                , 95.4f*8, quadrillagePiece);
        canvas.drawLine(100*8
                , 0
                , 100*8
                , 477*2, quadrillagePiece);

        // affiche les Ibeacons
        Paint pointBeacon = new Paint();
        pointBeacon.setColor(Color.GREEN);
        canvas.drawCircle( planPieceQuatre.getPositionBeaconA().x
                , planPieceQuatre.getPositionBeaconA().y, 50, pointBeacon);
        canvas.drawCircle( planPieceQuatre.getPositionBeaconB().x
                , planPieceQuatre.getPositionBeaconB().y, 50, pointBeacon);
        canvas.drawCircle( planPieceQuatre.getPositionBeaconC().x
                , planPieceQuatre.getPositionBeaconC().y, 50, pointBeacon);
        canvas.drawCircle( planPieceQuatre.getPositionBeaconD().x
                , planPieceQuatre.getPositionBeaconD().y, 50, pointBeacon);

        // afficher les points
        if (position != null) {
            Paint pointBleu = new Paint();
            pointBleu.setColor(Color.GRAY);
            pointBleu.setStrokeWidth(10);
            //TODO *2
            canvas.drawRect(position.x,position.y,(position.x+200),(position.y+192),pointBleu);
        }
    }

    private Point quelZone(Point point) {
        //TODO fait
        int x = (point.x/200)*200;
        int y = (point.y/192)*192;
        return new Point(x,y);
    }


    @Override
    protected void onDestroy() {
        //sendEmail();
        super.onDestroy();
        stopScan();
        beaconManager.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (enCoursDeScan) {
            startScan();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (enCoursDeScan) {
            startScan();
        }
    }


    /**
     * //TODO
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == CODE_ACTIVATION_BLUETOOTH) {
            if(resultCode == Activity.RESULT_OK) {
                connect();
            } else {
                Toast.makeText(this, "Erreur activation Bluetooth", Toast.LENGTH_LONG).show();
                getSupportActionBar().setSubtitle("Erreur activation Bluetooth");
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Lance la connection du BeaconManager et démarre le scan
     */
    private void connect() {
        try {
            beaconManager.connect(new OnServiceBoundListener() {
                @Override
                public void onServiceBound() throws RemoteException {
                    startScan();
                }
            });
        } catch (RemoteException e) {
            Log.d(TAG_DEBUG, e.getMessage());
        }
    }

    /**
     * Vérifie si votre GSM posséde le BLE et si il est allumé.
     * Si vous ne l'êtes pas elle tente de l'allumer.
     */
    private void verificationBluetooth() {
        // Verifie que on posséde le bluetooth LE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Votre gsm n'a pas le bluetooth LE", Toast.LENGTH_SHORT).show();
            //TODO quitter l'application si pas de BLE
        }

        if(!beaconManager.isBluetoothEnabled()) {
            final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, CODE_ACTIVATION_BLUETOOTH);
        } else if(beaconManager.isConnected()) {
            startScan();
        } else {
            connect();
        }
    }

    /*----------------------------------------------------------------------------------------*/
    /*----------------------------------    BEACON    ----------------------------------------*/
    /*----------------------------------------------------------------------------------------*/

    /**
     * Initialise l'utilisation des beacons, j'ai limité la Region au beacon de chez
     * Kontakt avec un Uuid F7826DA6-4FA2-4E98-8024-BC5B71E0893E.
     * Et c'est içi que je définie ce qu'il faut faire chaque fois que je détécte des beacons
     */
    private void initBeacon() {
        beaconManager = BeaconManager.newInstance(this);
        // limite le UUID
        beaconManager.addFilter(Filters.newProximityUUIDFilter(
                UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E")));
        // trie la liste de beacons par ordre croisant sur la distance
        //beaconManager.setDistanceSort(BeaconDevice.DistanceSort.ASC);
        beaconManager.setScanMode(BeaconManager.SCAN_MODE_LOW_LATENCY);
        // beaconManager.setForceScanConfiguration(new ForceScanConfiguration(1000*60*2,1000));
        // implement la méthode qui vas être appelé chaque fois que des beacons
        // sont trouvé
        beaconManager.registerRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<BeaconDevice> beaconDevices) {
                // si il y'a au moins un beacon trouvé..
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (beaconDevices.size() >= 4 && enCoursDeScan) {
                            for (BeaconDevice bd : beaconDevices) {
                                if (bd.getBeaconUniqueId().equals("TOyZ") && bd.getAccuracy()*100 >1){
                                    //trilateration.setDistanceBeaconXDQW(bd.getAccuracy() * 100);
                                    quadriLateration.setDistanceBeaconA(bd.getAccuracy()*100);
                                    x.setText("A = " + (int) (bd.getAccuracy() * 100) + "\n");
                                }
                                if (bd.getBeaconUniqueId().equals("xdQW") && bd.getAccuracy()*100 >1){
                                    //trilateration.setDistanceBeaconTOYZ(bd.getAccuracy() * 100);
                                    quadriLateration.setDistanceBeaconB(bd.getAccuracy() * 100);
                                    t.setText("B = " + (int) (bd.getAccuracy() * 100) + "\n");
                                }
                                if (bd.getBeaconUniqueId().equals("WMkW") && bd.getAccuracy()*100 >1){
                                    //trilateration.setDistanceBeaconWMKW(bd.getAccuracy() * 100);
                                    quadriLateration.setDistanceBeaconC(bd.getAccuracy() * 100);
                                    w.setText("C = " + (int) (bd.getAccuracy() * 100) + "\n");
                                }
                                if (bd.getBeaconUniqueId().equals("4mQm") && bd.getAccuracy()*100 >1){
                                    //trilateration.setDistanceBeaconWMKW(bd.getAccuracy() * 100);
                                    quadriLateration.setDistanceBeaconD(bd.getAccuracy() * 100);
                                    z.setText("D = " + (int) (bd.getAccuracy() * 100) + "\n");
                                }
                            }
                            Point pGsm = quadriLateration.getPositionGsm();
                            // if faut laisser car pas mal de points ce retrouventhors de la piece
                            if (pGsm.x < planPieceQuatre.getPositionBeaconA().x)
                                pGsm.x = Math.abs(pGsm.x);
                            if (pGsm.y < planPieceQuatre.getPositionBeaconA().y)
                                pGsm.y = Math.abs(pGsm.y);
                            if (pGsm.x > planPieceQuatre.getPositionBeaconD().x)
                                pGsm.x = planPieceQuatre.getPositionBeaconD().x - (pGsm.x - planPieceQuatre.getPositionBeaconD().x);
                            if (pGsm.y > planPieceQuatre.getPositionBeaconD().y)
                                pGsm.y = planPieceQuatre.getPositionBeaconD().y - (pGsm.y - planPieceQuatre.getPositionBeaconD().y);

                            Point zone = quelZone(pGsm);
                            if (zone != null)
                                zones.add(zone);
                        }
                    }
                });
            }
        });
    }


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    private void calculerPoint() {

        if (!zones.isEmpty()) {
            HashMap<Point,Integer> map = new HashMap<Point,Integer>();
            int max=0;
            Point pMax= null;
            for (Point p:zones) {
                if (map.containsKey(p))
                    map.put(p,  map.get(p)+1);
                else
                    map.put(p, 1);
                max = map.get(p);
                pMax = p;
            }
            for (Point p:zones) {
                if (map.get(p) > max) {
                    max = map.get(p);
                    pMax = p;
                }
            }

            int pct =(int) (((double)max/zones.size())*100);
            coin.setText("Pct="+pct+ "\n");
            coin.append("X " + pMax.x + " Y " + pMax.y);
            afficherPlan(pMax, PlanPieceQuatre.Coin.COIN_HAUT_DROITE);
            zones.clear();
        } else {
            coin.setText("X = null Y = null\n");
            textMessage += "X = null Y = null\n\n";
        }
    }

    /**
     * Initialisation des boutons et autre composant de la fenetre principale.
     * Ici il y'en à aucun
     */
    private void initElements() {
        //TODO 1000 -> 500 954/477
        planPieceQuatre = new PlanPieceQuatre(new Point(0,0), new Point(1000,954));
        planPieceQuatre.setPositionBeaconA(new Point(0, 0));
        planPieceQuatre.setPositionBeaconB(new Point(0, 954));
        planPieceQuatre.setPositionBeaconC(new Point(1000, 0));
        planPieceQuatre.setPositionBeaconD(new Point(1000, 954));

        quadriLateration = new QuadriLateration(planPieceQuatre.getPositionBeaconA(),planPieceQuatre.getPositionBeaconB()
                ,planPieceQuatre.getPositionBeaconC(),planPieceQuatre.getPositionBeaconD());
        w = (TextView) findViewById(R.id.w);
        x = (TextView) findViewById(R.id.x);
        t = (TextView) findViewById(R.id.t);
        z = (TextView) findViewById(R.id.z);
        coin = (TextView) findViewById(R.id.coin);

        listePosition = new ArrayList<>();
    }

    /**
     * Démarre le scan des beacons.
     */
    private void startScan() {
        try {
            beaconManager.startRanging();
            enCoursDeScan = true;
            startTimer();
            Log.d(TAG_DEBUG,"Start Scan");
        } catch (RemoteException e) {
            Log.d(TAG_DEBUG,"Erreur de démarrage Scan");
        }
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 100, 5000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Stop le scan des beacons.
     */
    private void stopScan() {
        beaconManager.stopRanging();
        enCoursDeScan = false;
        stoptimertask();
        Log.d(TAG_DEBUG,"Stop Scan");
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //show the toast
                        calculerPoint();
                    }
                });
            }
        };
    }

}
