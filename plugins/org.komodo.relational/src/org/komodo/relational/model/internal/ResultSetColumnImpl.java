/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.model.internal;

import org.komodo.relational.Messages;
import org.komodo.relational.Messages.Relational;
import org.komodo.relational.RelationalConstants;
import org.komodo.relational.RelationalConstants.Nullable;
import org.komodo.relational.RelationalProperties;
import org.komodo.relational.internal.AdapterFactory;
import org.komodo.relational.internal.RelationalChildRestrictedObject;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.relational.model.ResultSetColumn;
import org.komodo.relational.model.StatementOption;
import org.komodo.relational.model.TabularResultSet;
import org.komodo.repository.ObjectImpl;
import org.komodo.spi.KException;
import org.komodo.spi.repository.Descriptor;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.Property;
import org.komodo.spi.repository.PropertyDescriptor;
import org.komodo.spi.repository.PropertyValueType;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.spi.repository.Repository.UnitOfWork.State;
import org.komodo.utils.StringUtils;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlLexicon.CreateProcedure;

/**
 * An implementation of a relational model tabular result set column.
 */
public final class ResultSetColumnImpl extends RelationalChildRestrictedObject implements ResultSetColumn {

    private enum StandardOption {

        ANNOTATION,
        NAMEINSOURCE,
        UUID;

        /**
         * @param name
         *        the name being checked (can be <code>null</code>)
         * @return <code>true</code> if the name is the name of a standard option
         */
        static boolean isValid( final String name ) {
            for ( final StandardOption option : values() ) {
                if ( option.name().equals( name ) ) {
                    return true;
                }
            }

            return false;
        }

        /**
         * @return the names of all the options (never <code>null</code> or empty)
         */
        static String[] names() {
            final StandardOption[] options = values();
            final String[] result = new String[ options.length ];
            int i = 0;

            for ( final StandardOption option : options ) {
                result[i++] = option.name();
            }

            return result;
        }

    }

    /**
     * The resolver of a {@link ResultSetColumn}.
     */
    public static final TypeResolver< ResultSetColumn > RESOLVER = new TypeResolver< ResultSetColumn >() {

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#create(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.Repository, org.komodo.spi.repository.KomodoObject, java.lang.String,
         *      org.komodo.relational.RelationalProperties)
         */
        @Override
        public ResultSetColumn create( final UnitOfWork transaction,
                                       final Repository repository,
                                       final KomodoObject parent,
                                       final String id,
                                       final RelationalProperties properties ) throws KException {
            final AdapterFactory adapter = new AdapterFactory( repository );
            final TabularResultSet parentResultSet = adapter.adapt( transaction, parent, TabularResultSet.class );

            if ( parentResultSet == null ) {
                throw new KException( Messages.getString( Relational.INVALID_PARENT_TYPE,
                                                          parent.getAbsolutePath(),
                                                          ResultSetColumn.class.getSimpleName() ) );
            }

            return parentResultSet.addColumn( transaction, id );
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
        public Class< ResultSetColumnImpl > owningClass() {
            return ResultSetColumnImpl.class;
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
            return ObjectImpl.validateType( transaction, kobject.getRepository(), kobject, CreateProcedure.RESULT_COLUMN );
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.spi.repository.Repository.UnitOfWork,
         *      org.komodo.spi.repository.KomodoObject)
         */
        @Override
        public ResultSetColumn resolve( final UnitOfWork transaction,
                                        final KomodoObject kobject ) throws KException {
            return new ResultSetColumnImpl( transaction, kobject.getRepository(), kobject.getAbsolutePath() );
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
     *         if an error occurs or if node at specified path is not a column
     */
    public ResultSetColumnImpl( final UnitOfWork uow,
                                final Repository repository,
                                final String workspacePath ) throws KException {
        super( uow, repository, workspacePath );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#getCustomOptions(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public StatementOption[] getCustomOptions( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getCustomOptions( transaction, this );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getDatatypeName(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getDatatypeName( final UnitOfWork uow ) throws KException {
        final String value = getObjectProperty( uow, PropertyValueType.STRING, "getDatatypeName", //$NON-NLS-1$
                                                StandardDdlLexicon.DATATYPE_NAME );

        if ( StringUtils.isBlank( value ) ) {
            return RelationalConstants.DEFAULT_DATATYPE_NAME;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getDefaultValue(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getDefaultValue( final UnitOfWork uow ) throws KException {
        return getObjectProperty( uow, PropertyValueType.STRING, "getDefaultValue", StandardDdlLexicon.DEFAULT_VALUE ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getDescription(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getDescription( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getOption( transaction, this, StandardOption.ANNOTATION.name() );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getLength(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public long getLength( final UnitOfWork uow ) throws KException {
        final Long value = getObjectProperty( uow, PropertyValueType.LONG, "getLength", StandardDdlLexicon.DATATYPE_LENGTH ); //$NON-NLS-1$

        if ( value == null ) {
            return RelationalConstants.DEFAULT_LENGTH;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getNameInSource(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getNameInSource( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getOption( transaction, this, StandardOption.NAMEINSOURCE.name() );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getNullable(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Nullable getNullable( final UnitOfWork uow ) throws KException {
        final String value = getObjectProperty( uow, PropertyValueType.STRING, "getNullable", //$NON-NLS-1$
                                                StandardDdlLexicon.NULLABLE );

        if ( StringUtils.isBlank( value ) ) {
            return Nullable.DEFAULT_VALUE;
        }

        return Nullable.fromValue( value );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getPrecision(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public long getPrecision( final UnitOfWork uow ) throws KException {
        final Long value = getObjectProperty( uow, PropertyValueType.LONG, "getPrecision", //$NON-NLS-1$
                                                 StandardDdlLexicon.DATATYPE_PRECISION );

        if ( value == null ) {
            return RelationalConstants.DEFAULT_PRECISION;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#getPrimaryType(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public Descriptor getPrimaryType( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.createPrimaryType(transaction, this, super.getPrimaryType( transaction ));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#getProperty(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public Property getProperty( final UnitOfWork transaction,
                                 final String name ) throws KException {
        return OptionContainerUtils.getProperty( transaction, this, name, super.getProperty( transaction, name ) );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#getPropertyDescriptor(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public PropertyDescriptor getPropertyDescriptor( final UnitOfWork transaction,
                                                     final String propName ) throws KException {
        return OptionContainerUtils.getPropertyDescriptor( transaction,
                                                           this,
                                                           propName,
                                                           super.getPropertyDescriptor( transaction, propName ) );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#getPropertyNames(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String[] getPropertyNames( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getPropertyNames( transaction, this, super.getPropertyNames( transaction ) );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#getScale(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public long getScale( final UnitOfWork uow ) throws KException {
        final Long value = getObjectProperty( uow, PropertyValueType.LONG, "getScale", //$NON-NLS-1$
                                                 StandardDdlLexicon.DATATYPE_SCALE );

        if ( value == null ) {
            return RelationalConstants.DEFAULT_SCALE;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#getStandardOptionNames()
     */
    @Override
    public String[] getStandardOptionNames() {
        return StandardOption.names();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#getStatementOptionNames(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String[] getStatementOptionNames( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getOptionNames( transaction, this );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#getStatementOptions(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public StatementOption[] getStatementOptions( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getOptions( transaction, this );
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
     * @see org.komodo.relational.model.ResultSetColumn#getUuid(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public String getUuid( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.getOption( transaction, this, StandardOption.UUID.name() );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#hasProperties(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public boolean hasProperties( final UnitOfWork transaction ) throws KException {
        return OptionContainerUtils.hasProperties( transaction, this, super.hasProperties( transaction ) );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.internal.RelationalObjectImpl#hasProperty(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public boolean hasProperty( final UnitOfWork transaction,
                                final String name ) throws KException {
        return OptionContainerUtils.hasProperty( transaction, this, name, super.hasProperty( transaction, name ) );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#isCustomOption(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public boolean isCustomOption( final UnitOfWork transaction,
                                   final String name ) throws KException {
        return OptionContainerUtils.hasCustomOption( transaction, this, name );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#isStandardOption(java.lang.String)
     */
    @Override
    public boolean isStandardOption( final String name ) {
        return StandardOption.isValid( name );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#removeStatementOption(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void removeStatementOption( final UnitOfWork transaction,
                                       final String optionToRemove ) throws KException {
        OptionContainerUtils.removeOption( transaction, this, optionToRemove );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setDatatypeName(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void setDatatypeName( final UnitOfWork uow,
                                 final String newTypeName ) throws KException {
        setObjectProperty( uow, "setDatatypeName", StandardDdlLexicon.DATATYPE_NAME, newTypeName ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setDefaultValue(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void setDefaultValue( final UnitOfWork uow,
                                 final String newDefaultValue ) throws KException {
        setObjectProperty( uow, "setDefaultValue", StandardDdlLexicon.DEFAULT_VALUE, newDefaultValue ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setDescription(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void setDescription( final UnitOfWork transaction,
                                final String newDescription ) throws KException {
        setStatementOption( transaction, StandardOption.ANNOTATION.name(), newDescription );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setLength(org.komodo.spi.repository.Repository.UnitOfWork, long)
     */
    @Override
    public void setLength( final UnitOfWork uow,
                           final long newLength ) throws KException {
        setObjectProperty( uow, "setLength", StandardDdlLexicon.DATATYPE_LENGTH, newLength ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setNameInSource(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String)
     */
    @Override
    public void setNameInSource( final UnitOfWork transaction,
                                 final String newNameInSource ) throws KException {
        setStatementOption( transaction, StandardOption.NAMEINSOURCE.name(), newNameInSource );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setNullable(org.komodo.spi.repository.Repository.UnitOfWork,
     *      org.komodo.relational.RelationalConstants.Nullable)
     */
    @Override
    public void setNullable( final UnitOfWork uow,
                             final Nullable newNullable ) throws KException {
        setObjectProperty( uow, "setNullable", //$NON-NLS-1$
                           StandardDdlLexicon.NULLABLE,
                           ( newNullable == null ) ? Nullable.DEFAULT_VALUE.toValue() : newNullable.toValue() );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setPrecision(org.komodo.spi.repository.Repository.UnitOfWork, long)
     */
    @Override
    public void setPrecision( final UnitOfWork uow,
                              final long newPrecision ) throws KException {
        setObjectProperty( uow, "setPrecision", StandardDdlLexicon.DATATYPE_PRECISION, newPrecision ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.repository.ObjectImpl#setProperty(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String,
     *      java.lang.Object[])
     */
    @Override
    public void setProperty( final UnitOfWork transaction,
                             final String propertyName,
                             final Object... values ) throws KException {
        // if an option was not set then set a property
        if ( !OptionContainerUtils.setProperty( transaction, this, propertyName, values ) ) {
            super.setProperty( transaction, propertyName, values );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setScale(org.komodo.spi.repository.Repository.UnitOfWork, long)
     */
    @Override
    public void setScale( final UnitOfWork uow,
                          final long newScale ) throws KException {
        setObjectProperty( uow, "setScale", StandardDdlLexicon.DATATYPE_SCALE, newScale ); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.OptionContainer#setStatementOption(org.komodo.spi.repository.Repository.UnitOfWork,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public StatementOption setStatementOption( final UnitOfWork transaction,
                                               final String optionName,
                                               final String optionValue ) throws KException {
        return OptionContainerUtils.setOption( transaction, this, optionName, optionValue );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.model.ResultSetColumn#setUuid(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     */
    @Override
    public void setUuid( final UnitOfWork transaction,
                         final String newUuid ) throws KException {
        setStatementOption( transaction, StandardOption.UUID.name(), newUuid );
    }

}
