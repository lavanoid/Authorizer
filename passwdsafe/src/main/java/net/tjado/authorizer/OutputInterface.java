/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.authorizer;

public interface OutputInterface {
    public enum Language { en_US, de_DE, AppleMac_de_DE};
    public boolean setLanguage(OutputInterface.Language lang);
    public int sendText(String text) throws Exception;
    public void destruct() throws Exception;
}