/*******************************************************************************
 *     Copyright (C) 2017 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.core.script.lexer;

public class LexerException extends Exception {
    private static final long serialVersionUID = 1L;

    public LexerException(Lexer lexer) {
        super("An Error near row:" + lexer.getRow() + " col:" + lexer.getCol());
    }

    public LexerException(String message, Lexer lexer) {
        super(message + " near row:" + lexer.getRow() + " col:" + lexer.getCol());
    }
}
