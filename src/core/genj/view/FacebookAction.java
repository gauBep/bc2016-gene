package genj.view;

import genj.renderer.RenderSelectionHintKey;
import genj.tree.TreeView;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;
import genj.util.swing.ImageIcon;
import genj.util.swing.DialogHelper.Dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.restfb.BinaryAttachment;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.FacebookType;
import com.restfb.types.User;
import com.sun.xml.internal.ws.api.Component;
import com.teamdev.jxbrowser.chromium.Browser;

/**
 * An action for copying to an image
 */
public class FacebookAction extends Action2 {

	private final static ImageIcon IMG2 = new ImageIcon(FacebookAction.class, "images/Facebook.png");
	private final static Resources RES2 = Resources.get(FacebookAction.class);

	private JComponent component;
	private static Toolkit toolkit;

	public FacebookAction(JComponent component) {
		setImage(IMG2);
		//setTip(RES.getString("screenshot"));
		this.component = component;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Rectangle rVisible = component.getVisibleRect();
		Rectangle r = new Rectangle(new Point(), component.getSize());
		
		// Create image & copy
		try {
			BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderSelectionHintKey.KEY, false);
			g.setClip(0, 0, r.width, r.height);
			g.translate(-r.x, -r.y);
			component.paint(g);
			g.dispose();
			ImageTransferable imageSelection = new ImageTransferable(image);
			toolkit = Toolkit.getDefaultToolkit();
			toolkit.getSystemClipboard().setContents(imageSelection, null);
		} catch (OutOfMemoryError oom) {
			long max = Runtime.getRuntime().maxMemory() / 1024 / 1000;
			String msg = RES2.getString("screenshot.oom", r.width * r.height * 4 / 1024 / 1000, max,
					String.valueOf(max));
			Logger.getLogger("genj.view").log(Level.WARNING, msg, oom);
			DialogHelper.openDialog(getTip(), DialogHelper.ERROR_MESSAGE, msg, Action2.okOnly(), e);
		}
		// done
		
		//if (r.width > rVisible.width || r.height > rVisible.height) {
			JLabel loginLabel = new JLabel("Login:    ");
			loginLabel.setHorizontalAlignment(JLabel.RIGHT);
			JTextField login = new JTextField();
			login.setText("genealogyJava@gmail.com");
			JLabel passwordLabel = new JLabel("Password:    ");
			passwordLabel.setHorizontalAlignment(JLabel.RIGHT);
			JPasswordField password = new JPasswordField();
			password.setText("genJp@ss");
			
			JButton sendForAcceptance = new JButton("Send for acceptance");
			
			JButton postToFacebook = new JButton("Post to Facebook");
			
			final JPanel layout = new JPanel(new GridLayout(3, 2));
			
			layout.add(loginLabel);
			layout.add(login);
			layout.add(passwordLabel);
			layout.add(password);
			layout.add(sendForAcceptance);
			layout.add(postToFacebook);
			
			final Dialog dialog = DialogHelper.getClosableDialog("Post family tree to Facebook", DialogHelper.QUESTION_MESSAGE, layout,
					Action2.okCancel(), e);
			
			sendForAcceptance.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sendForAcceptanceButtonPressed(dialog);
				}
			});
			
			postToFacebook.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					postToFacebookButtonPressed(login.getText(), password.getPassword(), dialog);
				}
			});
	
			if (0 != dialog.show())
				return;
		//}
	}
	
	private void postToFacebookButtonPressed(String login, char[] password, Dialog dialog) {
		boolean logged = false;
		dialog.close(0);
		FacebookTokenizer facebookTokenizer = new FacebookTokenizer();
		try {
			facebookTokenizer.login(login, String.valueOf(password));	
			logged = true;
		} catch (Exception e) {
			String msg = "ERROR while login to Facebook!";
			Logger.getLogger("genj.view").log(Level.WARNING, msg, e);
			DialogHelper.openDialog(getTip(), DialogHelper.ERROR_MESSAGE, msg, Action2.okOnly(), e);			
		}
		
		if (logged)
		try {
			// System.out.println(facebookTokenizer.writeInWall("me/feed", "TEST MESSAGE7"));
			FacebookClient fbClient = new DefaultFacebookClient(facebookTokenizer.getAccessToken());
			User me = fbClient.fetchObject("me", User.class);
			System.out.println(me.getName());
//			fbClient.publish("me/feed", FacebookType.class, 
//					Parameter.with("link", "http://genj.sourceforge.net"));
			InputStream data = (InputStream) toolkit.getSystemClipboard().getData(DataFlavor.imageFlavor);
			FacebookType publishPhotoResponse = fbClient.publish("me/photos", FacebookType.class,
					  BinaryAttachment.with("FamilyTree.png", data),
					  // getClass().getResourceAsStream("/cat.png")
					  Parameter.with("message", "Test cat"));
			System.out.println("Published photo ID: " + publishPhotoResponse.getId());
		} catch (Exception e) {
			String msg = "ERROR while posting to Facebook!";
			Logger.getLogger("genj.view").log(Level.WARNING, msg, e);
			DialogHelper.openDialog(getTip(), DialogHelper.ERROR_MESSAGE, msg, Action2.okOnly(), e);				
		}
	}	
	
	private void sendForAcceptanceButtonPressed(Dialog dialog) {
		dialog.close(0);
		final Browser browser = new Browser();
		com.teamdev.jxbrowser.chromium.swing.BrowserView view = new com.teamdev.jxbrowser.chromium.swing.BrowserView(browser);
	    
		JFrame frame = new JFrame("http://www.facebook.com");
		frame.requestFocus();

		frame.setSize(600, 600);
		frame.setVisible(true);	
		frame.setAlwaysOnTop(true);
		frame.requestFocus();
		frame.add(view);

		browser.loadURL("https://www.facebook.com/dialog/permissions.request?_path=permissions.request" +
						"&app_id=1345837392094044&redirect_uri=https://www.facebook.com/connect/login_success.html" + 
						"&response_type=token&perms=public_profile,publish_actions");
		
	}

	/**
	 * A Transferable able to transfer an AWT Image. Similar to the JDK
	 * StringSelection class.
	 */
	private static class ImageTransferable implements Transferable {

		private Image image;

		private ImageTransferable(Image image) {
			this.image = image;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (flavor.equals(DataFlavor.imageFlavor) == false)
				throw new UnsupportedFlavorException(flavor);
			return image;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(DataFlavor.imageFlavor);
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}
	}
}
