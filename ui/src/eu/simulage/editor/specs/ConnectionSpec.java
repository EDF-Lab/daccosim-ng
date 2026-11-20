/*
 *  Copyright 2013-2018 - Monentia
 *
 *   Daccosim is a collaborative development effort between EDF (France),
 *   CentraleSupélec (France), EIFER Institute (Germany), SIANI institute (Spain)
 *   and Monentia S.L. (Spain)
 *
 *  This File is part of Daccosim Project
 *
 * Daccosim Project is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License.
 *
 *  Daccosim Project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Daccosim. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.simulage.editor.specs;

import java.io.Serializable;

public class ConnectionSpec implements Serializable {
	private String from;
	private String to;

	@SuppressWarnings({"unused", "WeakerAccess"})
	public ConnectionSpec() {
	}

	ConnectionSpec(String from, String to) {
		this.from = from;
		this.to = to;
	}

	public String getFrom() {
		if(this.from == null) return null;
		String from = clean(this.from);
		return this.from = from;
	}

	@SuppressWarnings("unused")
	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		if(this.from == null) return null;
		String to = clean(this.to);
		return this.to = to;
	}

	@SuppressWarnings("unused")
	public void setTo(String to) {
		this.to = to;
	}

	private String clean(String from) {
		return from
				.replace(" (Real)", "")
				.replace(" (Integer)", "")
				.replace(" (String)", "")
				.replace(" (Boolean)", "");
	}
}
