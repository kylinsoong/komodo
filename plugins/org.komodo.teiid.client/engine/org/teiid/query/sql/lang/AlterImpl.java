/* Generated By:JJTree: Do not edit this line. Alter.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import java.util.List;

import org.komodo.spi.query.sql.lang.Alter;
import org.teiid.query.parser.TCLanguageVisitorImpl;
import org.teiid.query.parser.TeiidClientParser;
import org.teiid.query.sql.symbol.BaseExpression;
import org.teiid.query.sql.symbol.GroupSymbolImpl;

/**
 *
 * @param <T>
 */
public abstract class AlterImpl<T extends CommandImpl> extends CommandImpl implements Alter<BaseExpression, TCLanguageVisitorImpl> {

    private GroupSymbolImpl target;

    private T definition;

    /**
     * @param p
     * @param id
     */
    public AlterImpl(TeiidClientParser p, int id) {
        super(p, id);
    }

    /**
     * @return the target
     */
    public GroupSymbolImpl getTarget() {
        return this.target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(GroupSymbolImpl target) {
        this.target = target;
    }

    /**
     * @return the definition
     */
    public T getDefinition() {
        return definition;
    }

    /**
     * @param definition the definition to set
     */
    public void setDefinition(T definition) {
        this.definition = definition;
    }

    @Override
    public List<BaseExpression> getProjectedSymbols() {
        return getUpdateCommandSymbol();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.definition == null) ? 0 : this.definition.hashCode());
        result = prime * result + ((this.target == null) ? 0 : this.target.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        AlterImpl other = (AlterImpl)obj;
        if (this.definition == null) {
            if (other.definition != null) return false;
        } else if (!this.definition.equals(other.definition)) return false;
        if (this.target == null) {
            if (other.target != null) return false;
        } else if (!this.target.equals(other.target)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(TCLanguageVisitorImpl visitor) {
        visitor.visit(this);
    }
}
/* JavaCC - OriginalChecksum=4c2a7e700d4af2b1569d4947a1d82223 (do not edit this line) */
