/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.client.piperemotefx;

import com.google.zxing.WriterException;
import com.phono.srtplight.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import pe.pi.client.small.App;
import pe.pi.client.small.screen.Qart;
import pe.pi.client.small.screen.SmallScreen;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import pe.pi.sctp4j.sctp.SCTPOutboundStreamOpenedListener;

/**
 *
 * @author thp
 */
public class LightSwitchController implements Initializable, SCTPStreamListener, SCTPOutboundStreamOpenedListener {

    private SCTPStream echo;

    @FXML
    private Group qrgroup;
    @FXML
    private Group switchgroup;
    @FXML
    private Canvas qrcanvas;
    @FXML
    private Label label;
    private int n;

    @FXML
    private void handleButtonAction(ActionEvent event) {
        System.out.println("You clicked me!");
        label.setText("Hello world");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Log.setLevel(Log.DEBUG);
        SmallScreen ss = new SmallScreen() {
            @Override
            public void init() throws UnsupportedOperationException {
            }

            @Override
            public void clearScreen() {
                Log.info("clear screen");
            }

            @Override
            public void drawQr(String q) {
                Log.info("drawQ called with " + q);
                Platform.runLater(() -> paintQR(q));
            }

            @Override
            public void showMessage(String mess) {
                Log.info("message " + mess);
            }

            @Override
            public void setStatus(String k, String v) {
                Log.info("status [" + k + "]=" + v);
                /*
                2) (S) is status - vales are c (communicating) O (got an offer) A
     * (sending an answer) C (sending a candidate - ipaddress) M (got a new
     * owner) X (rejected a new owner)
                 */
                if (k.equals("S") && v.equals("M")) {
                    Log.info("Added master");
                    Platform.exit();
                }
                if (k.equals("A") && v.equals("+")) {
                    Platform.runLater(() -> showSwitch());
                }

            }

        };
        BiFunction<String, SCTPStream, SCTPStreamListener> mapper = (String lab, SCTPStream s) -> {
            SCTPStreamListener ret = null;
            Log.error("No inbound streams - closing " + lab);
            return ret;
        };
        LightSwitchController that = this;
        AssociationListener onAss = new AssociationListener() {
            @Override
            public void onAssociated(Association asctn) {
                Log.info("Associated");
                try {
                    echo = asctn.mkStream("echo");
                    echo.setSCTPStreamListener(that);
                } catch (Exception ex) {
                    Log.error("can't open echo stream");
                }
            }

            @Override
            public void onDisAssociated(Association asctn) {
                Log.info("dis-Associated");
            }

            @Override
            public void onDCEPStream(SCTPStream stream, String string, int i) throws Exception {
            }

            @Override
            public void onRawStream(SCTPStream stream) {
            }

        };

        try {
            Log.info("friend is " + PipeRemoteFX.friend);
            App.connectOnceToFriend(ss, mapper, ".", onAss, PipeRemoteFX.friend);
        } catch (Exception ex) {
            Log.error("Problem with connectOnce " + ex.getMessage());
            if (Log.getLevel() >= Log.ERROR) {
                StringWriter sw = new StringWriter();
                PrintWriter w = new PrintWriter(sw);
                ex.printStackTrace(w);
                Log.error(sw.toString());
            }
        }
    }

    private void paintQR(String qid) {
        try {
            Canvas canvas = this.qrcanvas;
            Log.info("paint QR called with " + qid);
            Log.info("on canvas " + canvas.toString());

            GraphicsContext gc = canvas.getGraphicsContext2D();
            byte[][] a = Qart.getQr(qid, false);
            int w = a.length;
            int h = a[0].length;
            int pw = (int) canvas.getWidth();
            int xscale = (pw) / w;
            int ph = (int) canvas.getHeight();
            int yscale = (ph) / h;
            int scale = Math.min(xscale, yscale);
            int xmargin = (pw - (w * scale)) / 2;
            int ymargin = (ph - (h * scale)) / 2;
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, pw, ph);

            gc.setFill(Color.BLACK);
            int c;
            for (c = 0; c < a.length; c++) {
                int y = ymargin + (c * scale);
                byte[] d = a[c];
                int r;
                for (r = 0; r < d.length; r++) {
                    int x = xmargin + (r * scale);
                    if (d[r] > 0) {
                        gc.fillRect(x, y, scale + 1, scale + 1);
                    }
                }
            }
        } catch (WriterException ex) {
            Log.error(ex.getMessage());
            ex.printStackTrace();
        }
        showQR();
    }

    private void showQR() {
        allinvisible();
        qrgroup.setVisible(true);
    }

    private void showSwitch() {
        allinvisible();
        switchgroup.setVisible(true);
    }

    private void allinvisible() {
        switchgroup.setVisible(false);
        qrgroup.setVisible(false);
    }

    @Override
    public void onMessage(SCTPStream stream, String string) {
        Log.debug("got message " + string);
        n++;
        try {
            stream.send("hello " + n);
        } catch (Exception ex) {
            Log.error("can't send " + ex.getMessage());
        }
    }

    @Override
    public void close(SCTPStream stream) {
        Log.debug("closed " + stream.getLabel());
    }

    @Override
    public void opened(SCTPStream stream) {
        if (stream == echo) {
            Log.debug("sending first words on echo");
            try {
                echo.send("hello "+n);
            } catch (Exception ex) {
                Log.error("can't write to echo stream");
            }
        } else {
            Log.error("eeek, not echo stream in opened.");
            Log.error("stream ="+((stream!=null)?stream.toString():"null")+" vs "+((echo!=null)?echo.toString():null));
        }
    }

}
