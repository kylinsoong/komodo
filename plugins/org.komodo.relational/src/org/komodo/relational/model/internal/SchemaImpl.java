/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.komodo.relational.model.internal;

import java.util.Properties;
import javax.jcr.Node;
import org.komodo.core.KomodoLexicon;
import org.komodo.modeshape.visitor.DdlNodeVisitor;
import org.komodo.relational.RelationalProperties;
import org.komodo.relational.internal.RelationalObjectImpl;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.relational.model.Schema;
import org.komodo.relational.workspace.WorkspaceManager;
import org.komodo.repository.ObjectImpl;
import org.komodo.spi.KException;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.PropertyValueType;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.spi.repository.Repository.UnitOfWork.State;
import org.komodo.spi.runtime.version.TeiidVersionProvider;
import org.komodo.utils.ArgCheck;

/**
 * A named schema fragment
 */
public class SchemaImpl extends RelationalObjectImpl implements Schema {

    /**
     * The resolver of a {@link Schema}.
     */
    public static final TypeResolver< Schema > RESOLVER = new TypeResolver< Schema >() {

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#create(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.Repository, org.komodo.spi.repository.KomodoObject, java.lang.String,
         *      org.komodo.relational.RelationalProperties)
         */
        @Override
        public Schema create( final UnitOfWork transaction,
                              final Repository repository,
                              final KomodoObject parent,
                              final String id,
                              final RelationalProperties properties ) throws KException {
            final WorkspaceManager mgr = WorkspaceManager.getInstance( repository );
            return mgr.createSchema( transaction, parent, id );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#identifier()
         */
        @Override
        public KomodoType identifier() {
            return IDENTIFIER;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#owningClass()
         */
        @Override
        public Class< SchemaImpl > owningClass() {
            return SchemaImpl.class;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolvable(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public boolean resolvable( final UnitOfWork transaction,
                                   final KomodoObject kobject ) throws KException {
            return ObjectImpl.validateType( transaction, kobject.getRepository(), kobject, KomodoLexicon.Schema.NODE_TYPE );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public Schema resolve( final UnitOfWork transaction,
                               final KomodoObject kobject ) throws KException {
            return new SchemaImpl( transaction, kobject.getRepository(), kobject.getAbsolutePath() );
        }

    };

    /**
     * @param uow
     *        the transaction (cannot be <code>null</code> or have a state that is not {@link State#NOT_STARTED})
     * @param repository
     *        the repository where the relational object exists (cannot be <code>null</code>)
     * @param workspacePath
     *        the workspace relative path (cannot be empty)
     * @throws KException
     *         if an error occurs
     */
    public SchemaImpl( final UnitOfWork uow,
                       final Repository repository,
                       final String workspacePath ) throws KException {
        super( uow, repository, workspacePath );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.Exportable#export(org.komodo.spi.repository.Repository.UnitOfWork, java.util.Properties)
     */
    @Override
    public String export( final UnitOfWork transaction,
                          final Properties properties ) throws KException {
        // Is there a situation where this schema fragment is just Teiid SQL?
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        try {
            final StringBuffer result = new StringBuffer();
            final Node schemaNode = node( transaction );

            final DdlNodeVisitor visitor = new DdlNodeVisitor( TeiidVersionProvider.getInstance().getTeiidVersion(), false );
            visitor.visit( schemaNode );
            result.append( visitor.getDdl() );

            return result.toString();
        } catch ( final Exception e ) {
            throw handleError( e );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.Schema#getRendition(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getRendition( final UnitOfWork uow ) throws KException {
        final String rendition = getObjectProperty( uow, PropertyValueType.STRING, "getRendition", //$NON-NLS-1$
                                                    KomodoLexicon.Schema.RENDITION );

        return rendition == null ? EMPTY_STRING : rendition;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.spi.repository.KomodoObject#getTypeId()
     */
    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#getTypeIdentifier(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public KomodoType getTypeIdentifier( final UnitOfWork uow ) {
        return RESOLVER.identifier();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.Schema#setRendition(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setRendition( final UnitOfWork uow,
                              final String rendition ) throws KException {
        setObjectProperty( uow, "setRendition", KomodoLexicon.Schema.RENDITION, rendition ); //$NON-NLS-1$
    }

}
