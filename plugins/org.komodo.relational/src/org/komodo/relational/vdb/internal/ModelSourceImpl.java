/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.vdb.internal;

import org.komodo.relational.Messages;
import org.komodo.relational.Messages.Relational;
import org.komodo.relational.RelationalProperties;
import org.komodo.relational.internal.AdapterFactory;
import org.komodo.relational.internal.RelationalChildRestrictedObject;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.relational.model.Model;
import org.komodo.relational.vdb.ModelSource;
import org.komodo.repository.ObjectImpl;
import org.komodo.spi.KException;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.PropertyValueType;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.spi.repository.Repository.UnitOfWork.State;
import org.komodo.utils.ArgCheck;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * An implementation of a VDB model source.
 */
public final class ModelSourceImpl extends RelationalChildRestrictedObject implements ModelSource {

    /**
     * The resolver of a {@link ModelSource}.
     */
    public static final TypeResolver< ModelSource > RESOLVER = new TypeResolver< ModelSource >() {

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#create(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.Repository, org.komodo.spi.repository.KomodoObject, java.lang.String,
         *      org.komodo.relational.RelationalProperties)
         */
        @Override
        public ModelSource create( final UnitOfWork transaction,
                                   final Repository repository,
                                   final KomodoObject parent,
                                   final String id,
                                   final RelationalProperties properties ) throws KException {
            final AdapterFactory adapter = new AdapterFactory( repository );
            final Model parentModel = adapter.adapt( transaction, parent, Model.class );

            if ( parentModel == null ) {
                throw new KException( Messages.getString( Relational.INVALID_PARENT_TYPE,
                                                          parent.getAbsolutePath(),
                                                          ModelSource.class.getSimpleName() ) );
            }

            return parentModel.addSource( transaction, id );
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
        public Class< ModelSourceImpl > owningClass() {
            return ModelSourceImpl.class;
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
            return ObjectImpl.validateType( transaction, kobject.getRepository(), kobject, VdbLexicon.Source.SOURCE );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public ModelSource resolve( final UnitOfWork transaction,
                                    final KomodoObject kobject ) throws KException {
            return new ModelSourceImpl( transaction, kobject.getRepository(), kobject.getAbsolutePath() );
        }

    };

    /**
     * @param uow
     *        the transaction (cannot be <code>null</code> or have a state that is not {@link State#NOT_STARTED})
     * @param repository
     *        the repository where the relational object exists (cannot be <code>null</code>)
     * @param workspacePath
     *        the workspace path (cannot be empty)
     * @throws KException
     *         if an error occurs
     */
    public ModelSourceImpl( final UnitOfWork uow,
                            final Repository repository,
                            final String workspacePath ) throws KException {
        super( uow, repository, workspacePath );
    }

    @Override
    public KomodoType getTypeIdentifier( UnitOfWork uow ) {
        return RESOLVER.identifier();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.ModelSource#getJndiName(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getJndiName( final UnitOfWork uow ) throws KException {
        return getObjectProperty( uow, PropertyValueType.STRING, "getJndiName", VdbLexicon.Source.JNDI_NAME ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#getParent(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public KomodoObject getParent( final UnitOfWork transaction ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final KomodoObject grouping = super.getParent( transaction );
        final KomodoObject result = resolveType( transaction, grouping.getParent( transaction ) );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.ModelSource#getTranslatorName(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getTranslatorName( final UnitOfWork uow ) throws KException {
        return getObjectProperty( uow, PropertyValueType.STRING, "getTranslatorName", VdbLexicon.Source.TRANSLATOR ); //$NON-NLS-1$
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
     * @see org.komodo.relational.vdb.ModelSource#setJndiName(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setJndiName( final UnitOfWork uow,
                             final String newJndiName ) throws KException {
        setObjectProperty( uow, "setJndiName", VdbLexicon.Source.JNDI_NAME, newJndiName ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.vdb.ModelSource#setTranslatorName(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void setTranslatorName( final UnitOfWork uow,
                                   final String newTranslatorName ) throws KException {
        setObjectProperty( uow, "setTranslatorName", VdbLexicon.Source.TRANSLATOR, newTranslatorName ); //$NON-NLS-1$
    }

}
