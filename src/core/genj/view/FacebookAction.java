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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
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
	private static BufferedImage image;

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
			image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
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
			
			JLabel passwordLabel = new JLabel("Password:    ");
			passwordLabel.setHorizontalAlignment(JLabel.RIGHT);
			JPasswordField password = new JPasswordField();
			
			JButton sendForAcceptance = new JButton("Approve posting on my Wall");			
			JButton postToFacebook = new JButton("Post to Facebook");
			
			final JPanel layout = new JPanel(new GridLayout(3, 2));
			
			layout.add(loginLabel);
			layout.add(login);
			layout.add(passwordLabel);
			layout.add(password);
			layout.add(sendForAcceptance);
			layout.add(postToFacebook);
			
//			final Dialog dialog = DialogHelper.getClosableDialog("Post family tree to Facebook", 
//									DialogHelper.QUESTION_MESSAGE, layout,	Action2.okCancel(), e);	
						
			final JFrame postFrame = createFrame("Post family tree to Facebook", 300, 100);
			postFrame.add(layout);
			
			sendForAcceptance.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sendForAcceptanceButtonPressed(postFrame);
				}
			});
			
			postToFacebook.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					postToFacebookButtonPressed(login.getText(), password.getPassword(), postFrame);
				}
			});
	
//			if (0 != dialog.show())
//				return;
		//}
	}
	
    public JDialog ShowErrorMessage(String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = pane.createDialog("ERROR");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        return dialog;
    }

    public JDialog ShowInfoMessage(String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog("Info");
        dialog.setIconImage(IMG2.getImage());
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);        
        return dialog;
    }

	private void postToFacebookButtonPressed(String login, char[] password, JFrame frame) {
		JFrame info = createFrame("Connecting to your Facebook account...", 400, 80);
		boolean logged = false;
		frame.hide();
		FacebookTokenizer facebookTokenizer = new FacebookTokenizer();
		try {
			facebookTokenizer.login(login, String.valueOf(password));	
			logged = true;
		} catch (Exception e) {
			String msg = "ERROR while login to Facebook!";
			Logger.getLogger("genj.view").log(Level.WARNING, msg, e);
			ShowErrorMessage(msg);
		}		
		if (logged) {	
			info.hide();
	        JFrame content = createFrame("Please select post options", 250, 100);	        
	        JPanel layout = new JPanel(new GridLayout(3, 1));	        
	        
	        JCheckBox checkbox = new JCheckBox("Post link to GenJ program");
	        JPanel panel = new JPanel();
	        layout.add(checkbox);
	        JEditorPane edit = new JEditorPane();
	        JButton ok = new JButton("OK");
	        edit.setText("Please enter the post comment");
	        edit.setSize(new Dimension (content.getWidth(), content.getHeight() - 30));
	        
	        layout.add(edit);
	        layout.add(ok);
	        content.add(layout);	
	        
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					content.hide();
					postToFacebook(facebookTokenizer.getAccessToken(), checkbox.isSelected(), edit.getText());
				}
			});
		}			
			
	}	
	
	JFrame createFrame(String title, int x, int y) {
		JFrame frame = new JFrame(title);
		frame.setSize(x, y);
		frame.setLocationRelativeTo(null);
		frame.show();
		frame.setAlwaysOnTop(true);
		frame.setIconImage(IMG2.getImage());	
		return frame;
	}
	
	void postToFacebook(String token, boolean postLink, String comment){
		JFrame info = new JFrame();
		try {			
			info = createFrame("Please wait while connecting to Facebook...", 400, 80);
			FacebookClient fbClient = new DefaultFacebookClient(token);
			User me = fbClient.fetchObject("me", User.class);

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", byteStream);
			InputStream inputStream = new ByteArrayInputStream(byteStream.toByteArray());

			info.setTitle("Please wait while posting to Facebook...");
			if (postLink)
				fbClient.publish("me/feed", FacebookType.class, 
						Parameter.with("link", "http://genj.sourceforge.net"));

			FacebookType publishPhotoResponse = fbClient.publish("me/photos", FacebookType.class,
					BinaryAttachment.with("FamilyTree.png", inputStream), 
					Parameter.with("message", comment));
			// publishPhotoResponse.getId());
			info.hide();
			ShowInfoMessage("You message was sucessfully posted to Facebook!");
		} catch (Exception e) {
			info.hide();
			String msg = "ERROR while posting to Facebook!";
			Logger.getLogger("genj.view").log(Level.WARNING, msg, e);
			// DialogHelper.openDialog(getTip(), DialogHelper.ERROR_MESSAGE,
			// msg, Action2.okOnly(), e);			
			ShowErrorMessage(msg);
		}
	}
	
	private void sendForAcceptanceButtonPressed(JFrame frame) {
		//frame.hide();
		final Browser browser = new Browser();		
	    
		JFrame browserFrame = createFrame("Please wait while loading browser...", 600, 600);
		browserFrame.requestFocus();
				
		com.teamdev.jxbrowser.chromium.swing.BrowserView view = new com.teamdev.jxbrowser.chromium.swing.BrowserView(browser);
		browserFrame.add(view);
		browser.loadURL("https://www.facebook.com/dialog/permissions.request?_path=permissions.request" +
						"&app_id=1345837392094044&redirect_uri=https://www.facebook.com/connect/login_success.html" + 
						"&response_type=token&perms=public_profile,publish_actions");
		
		browserFrame.setTitle("www.facebook.com: Approve GenJ Application to post on my Wall");
		//frame.show();
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
