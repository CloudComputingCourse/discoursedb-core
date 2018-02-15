/*******************************************************************************
 * Copyright (C)  2015 - 2017  Carnegie Mellon University
 * Author: Oliver Ferschke, Chris Bogart
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.system.model.system;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.cmu.cs.lti.discoursedb.core.model.TimedBE;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * A DiscourseDB user's properties.
 * 
 * These are just a generic map from (type:string, name:string) -> value:string
 * used for keeping track of system_user session kind of stuff.
 * 
 * Main first use case is saved queries on discourse db data
 * 
 * @author Chris Bogart
 */
@Data
@Entity
@Table(name = "system_user_property")
public class SystemUserProperty extends TimedBE {

	@Id
	@Column(name = "id_system_user_property", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Setter(AccessLevel.PRIVATE)
	private Long id;

	@Override public String toString() { return super.toString(); }
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="fk_system_user", nullable=false)
	@JsonIgnore
	private SystemUser systemUser;
	
	private String propName;
	private String propType;
	
	@Lob
	@Column
	private String propValue;
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemUserProperty other = (SystemUserProperty) obj;
		
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
}
