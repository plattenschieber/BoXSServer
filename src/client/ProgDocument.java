package client;

import java.awt.Color;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;

public class ProgDocument extends DefaultStyledDocument implements UndoableEditListener
{
    private static final long serialVersionUID = 1L;

    static final String[] keywords={"wait","waitTime","waitForPlayers","waitForExperimenter","display",
		"video","inputString","inputNumber","inputStringNC","inputNumberNC","button","buttonNC","checkbox",
		"choice","choiceNC","choiceRandomize","choiceRandomizeNC","assert","if","while","for",
		"style","clear","manualLayout","automaticLayout","enableInputHistory","disableInputHistory",
		"matchManual","matchPerfectStranger","matchStranger","matchAll","matchDone","matchHistoryClear",
		"slider","enableMouseTracking","disableMouseTracking",
		"eyetrackerInitialise","eyetrackerCalibrate","eyetrackerStart","eyetrackerStop","eyetrackerTrigger","locale","recordKeys"};

    static final String[] minorKeywords={"PI","opponent","role","group","username","time","timestring",
		"sum","mean","median","min","max","abs","sqrt","sin","cos","tan","exp","log",
		"round","round1","round2", "randomUniform", "randomGauss","randomUniformInteger"};

    public UndoManager undo = new UndoManager();


    public int size=12;
    private Element rootElement;
    private MutableAttributeSet normal, keyword, minorKeyword, comment, specialComment, string;
    
    
    public ProgDocument()
    {
        rootElement=getDefaultRootElement();
        updateStyles();
        addUndoableEditListener(this);
    }



    public void updateStyles()
	{
        normal = new SimpleAttributeSet();
        StyleConstants.setFontSize(normal, size);
        StyleConstants.setForeground(normal, Color.black);

        keyword = new SimpleAttributeSet();
        StyleConstants.setFontSize(keyword, size);
        StyleConstants.setForeground(keyword, new Color(150,0,100));

        minorKeyword = new SimpleAttributeSet();
        StyleConstants.setFontSize(minorKeyword, size);
        StyleConstants.setItalic(minorKeyword, true);

        string = new SimpleAttributeSet();
        StyleConstants.setFontSize(string, size);
        StyleConstants.setForeground(string, new Color(0,0,200));

        comment = new SimpleAttributeSet();
        StyleConstants.setFontSize(comment, size);
        StyleConstants.setForeground(comment, new Color(120,120,120));

        specialComment = new SimpleAttributeSet();
        StyleConstants.setFontSize(specialComment, size);
        StyleConstants.setForeground(specialComment, new Color(120,120,120));
        StyleConstants.setItalic(specialComment, true);
	}



    boolean processing;
	public void processChangedLines(int _start, int _end)
    {
		processing=true;
		try
		{
		        String content = getText(0, getLength());
	        if (content.isEmpty()) return;
	    	if (_end>content.length()) _end=content.length()-1;

	        String lines[]=content.split("\n");
	        code_unnormal=new boolean[content.length()];

	        int offset=0;

	        for (int line=0; line<lines.length; line++)
	        {
	            int startOffset = rootElement.getElement( line ).getStartOffset();
	            int endOffset = rootElement.getElement( line ).getEndOffset() - 1;

	            int lineLength = endOffset - startOffset;
	            int contentLength = content.length();

	            if (endOffset >= contentLength)
	                endOffset = contentLength - 1;


	            //  set normal attributes for the line
	            if (lines[line].trim().startsWith("//"))
	            {
	                setCharacterAttributes(startOffset, lineLength, comment, true);
	                for (int i=startOffset; i<startOffset+lineLength; i++)
	                    code_unnormal[i]=true;

	                String start=lines[line].trim().substring(2).trim();
	                if (start.startsWith("Players: ") || start.startsWith("Name: ") ||
	                        start.startsWith("Author: ") || start.startsWith("Assign: "))
	                    setCharacterAttributes(startOffset+2, lineLength-2, specialComment, true);
	            }
	            else
	            {
	                setCharacterAttributes(startOffset, lineLength, normal, true);

	                // Kommentare
	                int pos=0;
	                while (lines[line].indexOf('\"',pos)!=-1)
	                {
	                    int pos1=lines[line].indexOf('\"',pos);
	                    int pos2=lines[line].indexOf('\"',pos1+1);

	                    if (pos2==-1)
	                    {
	                        setCharacterAttributes(offset+pos1, lineLength, string, true);
	                        pos=lineLength;
	                        for (int i=offset+pos1; i<offset+pos1+lineLength; i++)
	                            code_unnormal[i]=true;

	                    }
	                    else
	                    {
	                        setCharacterAttributes(offset+pos1, pos2-pos1+1, string, true);
	                        pos=pos2+1;
	                        for (int i=offset+pos1; i<offset+pos2; i++)
	                            code_unnormal[i]=true;
	                    }
	                }
	            }

	            offset+=lines[line].length()+1;
	        }

	        for (String kw:keywords)
	        {
	            int pos=0;
	            while (content.indexOf(kw, pos)!=-1)
	            {
	                int i=content.indexOf(kw, pos);
	                pos=i+1;
	                if (i>0 && content.charAt(i-1)!='\n' && content.charAt(i-1)!='\t' && content.charAt(i-1)!=' ')
	                	continue;
		            if (!code_unnormal[i])
	                    setCharacterAttributes(i, kw.length(), keyword, true);
	            }
	        }

	        for (String kw:minorKeywords)
	        {
	            int pos=0;
	            while (content.indexOf(kw, pos)!=-1)
	            {
	                int i=content.indexOf(kw, pos);
	                pos=i+1;
	                if (!code_unnormal[i])
	                    setCharacterAttributes(i, kw.length(), minorKeyword, true);
	            }
	        }
		}
		catch(Exception e)
		{
			;
		}
		processing=false;
    }


    boolean[] code_unnormal;
    /*
     *  Override to apply syntax highlighting after the document has been updated
     */
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException
    {
        super.insertString(offset, str, a);
        processChangedLines(offset, offset+str.length());
    }

    /*
     *  Override to apply syntax highlighting after the document has been updated
     */
    public void remove(int offset, int length) throws BadLocationException
    {
        super.remove(offset, length);
        processChangedLines(offset,offset+length);
    }


	@Override
	public void undoableEditHappened(UndoableEditEvent e)
	{
		if (!processing)
			undo.addEdit(e.getEdit());
	}

}
