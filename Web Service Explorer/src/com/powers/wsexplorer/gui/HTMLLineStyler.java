/*
 *   Copyright 2010 Nick Powers.
 *   This file is part of WSExplorer.
 *
 *   WSExplorer is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   WSExplorer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with WSExplorer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.powers.wsexplorer.gui;

import static com.Ostermiller.Syntax.Lexer.HTMLToken1.CHAR_REF;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.COMMENT;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.END_TAG_NAME;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.EQUAL;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.ERROR_MALFORMED_TAG;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.NAME;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.REFERENCE;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.SCRIPT;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.TAG_END;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.TAG_NAME;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.TAG_START;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.VALUE;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.WHITE_SPACE;
import static com.Ostermiller.Syntax.Lexer.HTMLToken1.WORD;
import static com.Ostermiller.Syntax.Lexer.Token.UNDEFINED_STATE;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;

import com.Ostermiller.Syntax.Lexer.HTMLLexer1;
import com.Ostermiller.Syntax.Lexer.HTMLToken1;
import com.powers.wsexplorer.ws.ColorFactory;

public class HTMLLineStyler implements LineStyleListener {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.custom.LineStyleListener#lineGetStyle(org.eclipse.swt.custom.LineStyleEvent)
	 */
	public void lineGetStyle(LineStyleEvent event) {
		HTMLLexer1 lexer = new HTMLLexer1(new StringReader(event.lineText));

		Color defaultFgColor = ((Control) event.widget).getForeground();
		HTMLToken1 token;
		
		List<StyleRange> styleRanges = new ArrayList<StyleRange>();
		if (event.styles != null && event.styles.length > 0) {
			styleRanges.addAll(Arrays.asList(event.styles));
		}

		try {
			while ((token = (HTMLToken1) lexer.getNextToken()) != null) {

				// ignore whitespace
				if (token.getID() == WHITE_SPACE) {
					continue;
				}

				Color color = getColor(token);
				if (!color.equals(defaultFgColor)) {

					final StyleRange style = new StyleRange(
							token.getCharBegin()+event.lineOffset, 
							token.getContents().length(),
							color, null);

					styleRanges.add(style);
				}

				event.styles = styleRanges.toArray(new StyleRange[0]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the color needed, by token.
	 * @param token
	 * @return
	 */
	public static Color getColor(HTMLToken1 token) {
		Color color = ColorFactory.getColor(Options.DefaultRGB);
		switch (token.getID()) {
		case COMMENT:
			color = ColorFactory.getColor(Options.CommentRGB);
			break;
		case NAME:
			color = ColorFactory.getColor(Options.AttributeRGB);
			break;
		case VALUE:
			color = ColorFactory.getColor(Options.ValueRGB);
			break;
		case WORD:
			color = ColorFactory.getColor(Options.TagValueRGB);
			break;
		case SCRIPT:
			color = ColorFactory.getColor(Options.ScriptRGB);
			break;
		case END_TAG_NAME:
		case TAG_END:
		case TAG_NAME:
		case TAG_START:
			color = ColorFactory.getColor(Options.TagRGB);
			break;
		case UNDEFINED_STATE:
			color = ColorFactory.getColor(Options.ErrorRGB);
			break;
		case ERROR_MALFORMED_TAG:
		case REFERENCE:
		case CHAR_REF:
		case EQUAL:
		default:
			// accept default
			break;
		}

		return color;
	}
	

}
