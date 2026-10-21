package com.windowmarkers;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;

/** Scroll only the form; keep confirmation controls outside its viewport. */
final class MarkerDialogs
{
    static Color chooseColor(Component parent,String title,Color initial)
    {
        JColorChooser chooser=new JColorChooser(initial);
        return confirm(parent,chooser,title,700,540) ? chooser.getColor() : null;
    }
    static boolean confirm(Component parent,JComponent content,String title,int width,int height)
    {
        Window owner=parent==null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        boolean[] accepted={false};
        JButton ok=new JButton("OK"),cancel=new JButton("Cancel");
        ok.addActionListener(e -> { accepted[0]=true; dialog.dispose(); });
        cancel.addActionListener(e -> dialog.dispose());
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        buttons.add(ok); buttons.add(cancel);
        JScrollPane scroll=new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setMinimumSize(new Dimension(0,0));
        JPanel root=new JPanel(new BorderLayout(0,10));
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(scroll,BorderLayout.CENTER); root.add(buttons,BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        GraphicsConfiguration configuration=owner==null ? dialog.getGraphicsConfiguration() : owner.getGraphicsConfiguration();
        Rectangle screen=configuration.getBounds();
        Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        Rectangle usable=new Rectangle(screen.x+insets.left,screen.y+insets.top,
            screen.width-insets.left-insets.right,screen.height-insets.top-insets.bottom);
        Rectangle bounds=initialBounds(usable,width,height);
        dialog.setMinimumSize(new Dimension(Math.min(320,bounds.width),Math.min(200,bounds.height)));
        dialog.setBounds(bounds);
        try { dialog.setVisible(true); return accepted[0]; }
        finally { dialog.dispose(); }
    }
    static Rectangle initialBounds(Rectangle screen,int width,int height)
    {
        int w=Math.min(width,Math.max(1,screen.width-24)),h=Math.min(height,Math.max(1,screen.height-24));
        return new Rectangle(screen.x+(screen.width-w)/2,screen.y+(screen.height-h)/2,w,h);
    }
}
