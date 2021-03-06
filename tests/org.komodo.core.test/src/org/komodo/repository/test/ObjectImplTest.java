/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.repository.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.komodo.spi.KException;
import org.komodo.spi.constants.StringConstants;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.test.utils.AbstractLocalRepositoryTest;
import org.modeshape.jcr.JcrNtLexicon;

@SuppressWarnings( {"javadoc", "nls"} )
public final class ObjectImplTest extends AbstractLocalRepositoryTest {

    private static final String NAME = "blah";
    KomodoObject kobject;

    @Before
    public void init() throws Exception {
        this.kobject = _repo.add( this.uow, null, NAME, null );
        commit();
    }

    @Test
    public void shouldAddChild() throws Exception {
        final String name = "kid";
        final String type = "nt:folder";
        final KomodoObject child = this.kobject.addChild( this.uow, name, type );
        assertThat( _repo.getFromWorkspace( this.uow, child.getAbsolutePath() ), is( notNullValue() ) );
        assertThat( child.getPrimaryType( this.uow ).getName(), is( type ) );
    }

    @Test
    public void shouldAddDescriptor() throws Exception {
        final String descriptorName = "mix:referenceable";
        this.kobject.addDescriptor( this.uow, descriptorName );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptorName ), is( true ) );
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( 1 ) );
        assertThat( this.kobject.getDescriptors( this.uow )[0].getName(), is( descriptorName ) );
    }

    @Test
    public void shouldAddMultipleDescriptors() throws Exception {
        final String descriptor1 = "mix:referenceable";
        final String descriptor2 = "mix:lockable";
        this.kobject.addDescriptor( this.uow, descriptor1, descriptor2 );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptor1 ), is( true ) );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptor2 ), is( true ) );
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( 2 ) );
    }

    @Test
    public void shouldExist() throws Exception {
        final KomodoObject obj = _repo.getFromWorkspace( this.uow, NAME );
        assertThat( obj, is( notNullValue() ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailAddingChildWithEmptyName() throws Exception {
        this.kobject.addChild( this.uow, EMPTY_STRING, null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailAddingChildWithNullName() throws Exception {
        this.kobject.addChild( this.uow, null, null );
    }

    @Test( expected = KException.class )
    public void shouldFailAddingEmptyDescriptorName() throws Exception {
        this.kobject.addDescriptor( this.uow, EMPTY_STRING );
    }

    @Test( expected = KException.class )
    public void shouldFailAddingNullDescriptorName() throws Exception {
        this.kobject.addDescriptor( this.uow, ( String )null );
    }

    @Test( expected = KException.class )
    public void shouldFailToGetChildIfItDoesNotExist() throws Exception {
        this.kobject.getChild( this.uow, "blah" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildWithEmptyName() throws Exception {
        this.kobject.getChild( this.uow, EMPTY_STRING );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildWithNullName() throws Exception {
        this.kobject.getChild( this.uow, null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildWithEmptyType() throws Exception {
        final String name = "kid";
        this.kobject.addChild( this.uow, name, null );
        this.kobject.getChild( this.uow, name, StringConstants.EMPTY_STRING );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildWithNullType() throws Exception {
        final String name = "kid";
        this.kobject.addChild( this.uow, name, null );
        this.kobject.getChild( this.uow, name, null );
    }

    @Test
    public void shouldGetChild() throws Exception {
        final String name = "kid";
        this.kobject.addChild( this.uow, name, null );
        assertThat( this.kobject.getChild( this.uow, name ), is( notNullValue() ) );
    }

    @Test( expected = KException.class )
    public void shouldFailToGetChildIfIncorrectType() throws Exception {
        final String name = "kid";
        this.kobject.addChild( this.uow, name, null );
        this.kobject.getChild( this.uow, name, JcrNtLexicon.FOLDER.getString() );
    }

    @Test
    public void shouldGetChildWithCorrectType() throws Exception {
        final String name = "kid";
        KomodoObject expected = this.kobject.addChild( this.uow, name, null );
        this.kobject.addChild( this.uow, name, JcrNtLexicon.FOLDER.getString() );
        assertThat( this.kobject.getChild( this.uow, name, JcrNtLexicon.UNSTRUCTURED.getString() ), is( expected ) );
    }

    @Test
    public void shouldGetIndex() throws Exception {
        assertThat( this.kobject.getIndex(), is( 0 ) );
    }

    @Test
    public void shouldGetNamedDescriptor() throws Exception {
        final String descriptorName = "mix:referenceable";
        this.kobject.addDescriptor( this.uow, descriptorName );
        this.kobject.addDescriptor( this.uow, "mix:lockable" );
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( 2 ) );
        assertThat( this.kobject.getDescriptor( this.uow, descriptorName ).getName(), is( descriptorName ) );
    }

    @Test
    public void shouldHaveSameNumberRawDescriptorsAsDescriptors() throws Exception {
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( this.kobject.getRawDescriptors( this.uow ).length ) );
    }

    @Test
    public void shouldHaveSameNumberRawPropertiesAsProperties() throws Exception {
        assertThat( this.kobject.getPropertyNames( this.uow ).length, is( this.kobject.getRawPropertyNames( this.uow ).length ) );
    }

    @Test
    public void shouldHaveUnknownTypeIdentifier() throws Exception {
        assertThat( this.kobject.getTypeIdentifier( this.uow ), is( KomodoType.UNKNOWN ) );
    }

    @Test
    public void shouldRemove() throws Exception {
        final KomodoObject obj = _repo.getFromWorkspace( this.uow, NAME );
        obj.remove( this.uow );
        assertThat( _repo.getFromWorkspace( this.uow, NAME ), is( nullValue() ) );
        assertThat( _repo.komodoWorkspace( this.uow ).getChildren( this.uow ).length, is( 0 ) );
    }

    @Test
    @Ignore("Mapping issue MODE-2463 - a remove then a re-add cannot be conducted in the same transaction")
    public void testRemoveThenAdd() throws Exception {
        String name = "testNode";

        UnitOfWork transaction1 = _repo.createTransaction("create-node-to-remove", false, null);
        KomodoObject wkspNode = _repo.komodoWorkspace(transaction1);
        assertNotNull(wkspNode);
        KomodoObject testNode = wkspNode.addChild(transaction1, name, null);
        assertNotNull(testNode);
        String testNodePath = testNode.getAbsolutePath();
        transaction1.commit();

        UnitOfWork transaction2 = _repo.createTransaction("node-removal", false, null);
        wkspNode = _repo.komodoWorkspace(transaction2);
        assertNotNull(wkspNode);
        testNode = _repo.getFromWorkspace(transaction2, testNodePath);
        assertNotNull(testNode);

        testNode.remove(transaction2);
        assertFalse(wkspNode.hasChild(transaction2, name));

        testNode = _repo.getFromWorkspace(transaction2, testNodePath);
        assertNull(testNode); 

        KomodoObject newTestNode = wkspNode.addChild(transaction2, name, null);

        /*
         * ISSUE #1
         *
         * This will fail with:
         * testNodePath = /{kworkspace}/testNode
         * testNode.getPath() = /{kworkspace}/testNode[2]
         */
        assertEquals(testNodePath, newTestNode.getAbsolutePath());        // Uncomment to view failure

        /*
         * ISSUE #2
         *
         * The path of newTestNode is alledgedly /{kworkspace}/testNode[2] so should
         * be able to find it from session2, except it fails
         */
        assertNotNull(_repo.getFromWorkspace(transaction2, testNodePath + "[2]")); //$NON-NLS-1$

        /*
         * ISSUE #3
         *
         * Despite newTestNode claiming its path is /{kworkspace}/testNode[2], transaction2
         * cannot find it so where is newTestNode?
         *
         * Turns out that its been added to /{kworkspace}/testNode which is the correct
         * path but not what is being reported by newTestNode.getPath()
         *
         * Conclusion: bug in node.getPath(), returning incorrect absolute path
         */
        KomodoObject kObject = _repo.getFromWorkspace(transaction2, testNodePath);
        assertEquals("Node path should equal " + testNodePath, testNodePath, kObject.getAbsolutePath()); //$NON-NLS-1$
        assertEquals(newTestNode.getAbsolutePath(), kObject.getAbsolutePath());
    }

    @Test
    public void shouldRemoveDescriptor() throws Exception {
        final String descriptorName = "mix:referenceable";
        this.kobject.addDescriptor( this.uow, descriptorName );
        this.kobject.removeDescriptor( this.uow, descriptorName );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptorName ), is( false ) );
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( 0 ) );
    }

    @Test
    public void shouldRemoveMultipleDescriptors() throws Exception {
        final String descriptor1 = "mix:referenceable";
        final String descriptor2 = "mix:lockable";
        this.kobject.addDescriptor( this.uow, descriptor1, descriptor2 );
        this.kobject.removeDescriptor( this.uow, descriptor1, descriptor2 );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptor1 ), is( false ) );
        assertThat( this.kobject.hasDescriptor( this.uow, descriptor2 ), is( false ) );
        assertThat( this.kobject.getDescriptors( this.uow ).length, is( 0 ) );
    }

    @Test
    public void shouldSetPrimaryType() throws Exception {
        final String newType = "nt:folder";
        this.kobject.setPrimaryType( this.uow, newType );
        assertThat( this.kobject.getPrimaryType( this.uow ).getName(), is( newType ) );
    }

}
