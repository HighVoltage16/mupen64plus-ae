/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.preference;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.io.File;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog.ScanRomsDialogListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class RomsFoldersActivity extends ListActivity
{
    /** The user preferences wrapper, available as a convenience to subclasses. */
    protected UserPrefs mUserPrefs;
    
    private final List<String> mFolders = new ArrayList<String>();
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshList();
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.profile_activity, menu );
        menu.findItem( R.id.menuItem_toggleBuiltins ).setVisible( false );
        return super.onCreateOptionsMenu( menu );
    }
    
    @TargetApi( 11 )
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_new:
                updateFolder( null, null );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }
    
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        // Popup a dialog with a context-sensitive list of options for the folder
        final String folder = (String) getListView().getItemAtPosition( position );
        if( folder != null )
        {
            int resId = R.array.romsFoldersClickCustom_entries;
            if ( mUserPrefs.romsDirs.length <= 1 )
                resId = R.array.romsFoldersClickCustomNoRemove_entries;
            
            CharSequence[] items = getResources().getTextArray( resId );
            
            Builder builder = new Builder( this );
            builder.setTitle( getString( R.string.popup_titleCustom, folder ) );
            builder.setItems( items, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    switch( which )
                    {
                        case 0:
                            updateFolder( folder, null );
                            break;
                        case 1:
                            removeFolder( folder );
                            break;
                    }
                }
            });
            builder.create().show();
        }
        super.onListItemClick( l, v, position, id );
    }
    
    private void updateFolder( String folder, File startDir )
    {
        if ( startDir == null && folder != null )
            startDir = new File( folder );
        
        // Prompt for search path for the ROMs folder
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        final String oldFolder = folder;
        ScanRomsDialog dialog = new ScanRomsDialog( this, startDir, true, new ScanRomsDialogListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    if ( oldFolder != null )
                        mUserPrefs.removeRomsFolder( oldFolder );
                    
                    mUserPrefs.addRomsFolder( file.getAbsolutePath() );
                    refreshList();
                }
                else if( file != null )
                {
                    updateFolder( oldFolder, file );
                }
            }
        });
        
        dialog.show();
    }
    
    private void removeFolder( String folder )
    {
        final String finalFolder = folder;
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.confirmRemoveFolder_message, folder );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                mUserPrefs.removeRomsFolder( finalFolder );
                refreshList();
            }
        } );
    }
    
    private void refreshList()
    {
        // Get the folders to be shown to the user
        setListAdapter( new RomsFoldersListAdapter( this, Arrays.asList( mUserPrefs.romsDirs ) ) );
    }
    
    private class RomsFoldersListAdapter extends ArrayAdapter<String>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        public RomsFoldersListAdapter( Context context, List<String> folders )
        {
            super( context, RESID, folders );
        }
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( RESID, null );
            
            String folder = getItem( position );
            if( folder != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                int index = folder.lastIndexOf( "/" );
                
                text1.setText( folder.substring( index + 1 ) );
                text2.setText( folder.substring( 0, index ) );
                icon.setImageResource( R.drawable.ic_folder );
            }
            return view;
        }
    }
}
