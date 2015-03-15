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
package paulscode.android.mupen64plusae.profile;

import org.apache.commons.lang.ArrayUtils;
import org.mupen64plusae.v3.alpha.R;
import java.lang.Runnable;

import paulscode.android.mupen64plusae.GameOverlay;
import paulscode.android.mupen64plusae.Keys;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.SettingsGlobalActivity;
import paulscode.android.mupen64plusae.dialog.SeekBarGroup;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.DrawerDrawable;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.support.v4.widget.DrawerLayout;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.Toolbar;

public class TouchscreenProfileActivity extends Activity implements OnTouchListener
{
    private static final String TOUCHSCREEN_AUTOHOLDABLES = "touchscreenAutoHoldables";
    private static final String AUTOHOLDABLES_DELIMITER = "~";
    
    private static final String ANALOG = "analog";
    private static final String DPAD = "dpad";
    private static final String GROUP_AB = "groupAB";
    private static final String GROUP_C = "groupC";
    private static final String BUTTON_L = "buttonL";
    private static final String BUTTON_R = "buttonR";
    private static final String BUTTON_Z = "buttonZ";
    private static final String BUTTON_S = "buttonS";
    private static final String TAG_X = "-x";
    private static final String TAG_Y = "-y";
    
    public static final SparseArray<String> READABLE_NAMES = new SparseArray<String>();
    
    // The inital or disabled x/y position of an asset
    private static final int INITIAL_ASSET_POS = 50;
    private static final int DISABLED_ASSET_POS = -1;
    
    // Touchscreen profile objects
    private ConfigFile mConfigFile;
    private Profile mProfile;
    
    // User preferences wrapper
    private UserPrefs mUserPrefs;
    
    // Visual elements
    private VisibleTouchMap mTouchscreenMap;
    private GameOverlay mOverlay;
    private ImageView mSurface;
    private DrawerLayout mDrawerLayout;
    private MenuListView mDrawerList;
    private Toolbar mToolbar;
    
    // Live drag and drop editing
    private int initialX;
    private int initialY;
    private int dragIndex;
    private boolean dragging;
    private String dragAsset;
    private int dragX;
    private int dragY;
    private Rect dragFrame;
    
    @SuppressLint( "ClickableViewAccessibility" )
    @TargetApi( 11 )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );
        super.onCreate( savedInstanceState );
        
        // Get the user preferences wrapper
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        // Load the profile; fail fast if there are any programmer usage errors
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "Invalid usage: bundle must indicate profile name" );
        String name = extras.getString( Keys.Extras.PROFILE_NAME );
        if( TextUtils.isEmpty( name ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        mConfigFile = new ConfigFile( mUserPrefs.touchscreenProfiles_cfg );
        ConfigSection section = mConfigFile.get( name );
        if( section == null )
            throw new Error( "Invalid usage: profile name not found in config file" );
        mProfile = new Profile( false, section );
        
        // Define the map from N64 button indices to readable button names
        READABLE_NAMES.put( AbstractController.DPD_R, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_L, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_D, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_U, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.START, getString( R.string.controller_buttonS ) );
        READABLE_NAMES.put( AbstractController.BTN_Z, getString( R.string.controller_buttonZ ) );
        READABLE_NAMES.put( AbstractController.BTN_B, getString( R.string.controller_buttonB ) );
        READABLE_NAMES.put( AbstractController.BTN_A, getString( R.string.controller_buttonA ) );
        READABLE_NAMES.put( AbstractController.CPD_R, getString( R.string.controller_buttonCr ) );
        READABLE_NAMES.put( AbstractController.CPD_L, getString( R.string.controller_buttonCl ) );
        READABLE_NAMES.put( AbstractController.CPD_D, getString( R.string.controller_buttonCd ) );
        READABLE_NAMES.put( AbstractController.CPD_U, getString( R.string.controller_buttonCu ) );
        READABLE_NAMES.put( AbstractController.BTN_R, getString( R.string.controller_buttonR ) );
        READABLE_NAMES.put( AbstractController.BTN_L, getString( R.string.controller_buttonL ) );
        READABLE_NAMES.put( TouchMap.DPD_LU, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_LD, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_RD, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_RU, getString( R.string.controller_dpad ) );
        
        // Enable full-screen mode
        getWindow().setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Lay out content and get the views
        setContentView( R.layout.touchscreen_profile_activity );
        mSurface = (ImageView) findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) findViewById( R.id.gameOverlay );
        
        // Configure the navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawerLayout );
        mToolbar = (Toolbar) findViewById( R.id.toolbar );
        mToolbar.setTitle( getString( R.string.menuItem_touchscreenProfiles ) );
        
        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );
        mDrawerList.setMenuResource( R.menu.touchscreen_profile_activity );
        mDrawerList.setBackgroundDrawable( new DrawerDrawable() );
        updateButtons();
        
        // Expand the Buttons group
        mDrawerList.expandGroup( 2 );
        
        // Handle menu item selections
        final Activity activity = this;
        mDrawerList.setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                switch( menuItem.getItemId() )
                {
                    case R.id.menuItem_globalSettings:
                        Intent intent = new Intent( activity, SettingsGlobalActivity.class );
                        intent.putExtra( Keys.Extras.MENU_DISPLAY_MODE, 1 );
                        startActivity( intent );
                        break;
                    case R.id.menuItem_exit:
                        finish();
                        break;
                    case R.id.menuItem_analog:
                        toggleAsset( ANALOG );
                        break;
                    case R.id.menuItem_dpad:
                        toggleAsset( DPAD );
                        break;
                    case R.id.menuItem_groupAB:
                        toggleAsset( GROUP_AB );
                        break;
                    case R.id.menuItem_groupC:
                        toggleAsset( GROUP_C );
                        break;
                    case R.id.menuItem_buttonL:
                        toggleAsset( BUTTON_L );
                        break;
                    case R.id.menuItem_buttonR:
                        toggleAsset( BUTTON_R );
                        break;
                    case R.id.menuItem_buttonZ:
                        toggleAsset( BUTTON_Z );
                        break;
                    case R.id.menuItem_buttonS:
                        toggleAsset( BUTTON_S );
                        break;
                }
            }
        });
        
        // Configure the action bar introduced in higher Android versions
        
        // Initialize the touchmap and overlay
        mTouchscreenMap = new VisibleTouchMap( getResources() );
        mOverlay.setOnTouchListener( this );
        mOverlay.initialize( mTouchscreenMap, true, mUserPrefs.isFpsEnabled, mUserPrefs.isTouchscreenAnimated );
        
        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor( 0x0 );
        
        // Make the background solid black
        mSurface.getRootView().setBackgroundColor( 0xFF000000 );
        
        if ( savedInstanceState == null )
        {
            // Show the drawer at the start and have it hide itself automatically
            mDrawerLayout.openDrawer( GravityCompat.START );
            mDrawerLayout.postDelayed( new Runnable()
            {
                public void run() {
                    mDrawerLayout.closeDrawer( GravityCompat.START );
                }
            }, 1000);
        }
    }
    
    @TargetApi( 11 )
    private void refresh()
    {
        // Reposition the assets and refresh the overlay and options menu
        mTouchscreenMap.load( mUserPrefs.touchscreenSkin, mProfile,
                mUserPrefs.isTouchscreenAnimated, true, mUserPrefs.touchscreenScale,
                mUserPrefs.touchscreenTransparency );
        mOverlay.postInvalidate();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Refresh in case the global settings changed
        mUserPrefs = new UserPrefs( this );
        
        // Update the dummy GameSurface size in case global settings changed
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        params.width = mUserPrefs.videoSurfaceWidth;
        params.height = mUserPrefs.videoSurfaceHeight;
        params.gravity = mUserPrefs.displayPosition | Gravity.CENTER_HORIZONTAL;
        mSurface.setLayoutParams( params );
        
        mDrawerList.getBackground().setAlpha( mUserPrefs.displaySidebarTransparency );
        
        // Refresh the touchscreen controls
        refresh();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        if( hasFocus )
            hideSystemBars();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        
        // Lazily persist the profile data; only need to do it on pause
        mProfile.writeTo( mConfigFile );
        mConfigFile.save();
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if ( keyCode == KeyEvent.KEYCODE_MENU )
        {
            if ( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                mDrawerLayout.closeDrawer( GravityCompat.START );
            else
                mDrawerLayout.openDrawer( GravityCompat.START );
            return true;
        }
        return super.onKeyDown( keyCode, event );
    }
    
    private void setCheckState( Menu menu, int id, String assetName )
    {
        MenuItem item = menu.findItem( id );
        if( item != null )
            item.setChecked( hasAsset( assetName ) );
    }
    
    private void updateButtons()
    {
        Menu menu = mDrawerList.getMenu();
        setCheckState( menu, R.id.menuItem_analog, ANALOG );
        setCheckState( menu, R.id.menuItem_dpad, DPAD );
        setCheckState( menu, R.id.menuItem_groupAB, GROUP_AB );
        setCheckState( menu, R.id.menuItem_groupC, GROUP_C );
        setCheckState( menu, R.id.menuItem_buttonL, BUTTON_L );
        setCheckState( menu, R.id.menuItem_buttonR, BUTTON_R );
        setCheckState( menu, R.id.menuItem_buttonZ, BUTTON_Z );
        setCheckState( menu, R.id.menuItem_buttonS, BUTTON_S );
        mDrawerList.reload();
    }
    
    private boolean hasAsset( String assetName )
    {
        // Get the asset position from the profile and see if it's valid
        int x = mProfile.getInt( assetName + TAG_X, DISABLED_ASSET_POS );
        int y = mProfile.getInt( assetName + TAG_Y, DISABLED_ASSET_POS );
        return ( x > DISABLED_ASSET_POS ) && ( y > DISABLED_ASSET_POS );
    }
    
    private void toggleAsset( String assetName )
    {
        // Change the position of the asset to show/hide
        int newPosition = hasAsset( assetName ) ? DISABLED_ASSET_POS : INITIAL_ASSET_POS;
        mProfile.putInt( assetName + TAG_X, newPosition );
        mProfile.putInt( assetName + TAG_Y, newPosition );
        
        updateButtons();
        refresh();
    }
    
    private void setHoldable( int n64Index, boolean holdable )
    {
        String index = String.valueOf( n64Index );
        
        // Get the serialized list from the profile
        String serialized = mProfile.get( TOUCHSCREEN_AUTOHOLDABLES, "" );
        String[] holdables = serialized.split( AUTOHOLDABLES_DELIMITER );
        
        // Modify the list as necessary
        if( !holdable )
        {
            holdables = (String[]) ArrayUtils.removeElement( holdables, index );
        }
        else if( !ArrayUtils.contains( holdables, index ) )
        {
            holdables = (String[]) ArrayUtils.add( holdables, index );
        }
        
        // Put the serialized list back into the profile
        serialized = TextUtils.join( AUTOHOLDABLES_DELIMITER, holdables );
        mProfile.put( TOUCHSCREEN_AUTOHOLDABLES, serialized );
    }
    
    private boolean getHoldable( int n64Index )
    {
        String serialized = mProfile.get( TOUCHSCREEN_AUTOHOLDABLES, "" );
        String[] holdables = serialized.split( AUTOHOLDABLES_DELIMITER );
        return ArrayUtils.contains( holdables, String.valueOf( n64Index ) );
    }
    
    @Override
    public void onBackPressed()
    {
        if ( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            mDrawerLayout.closeDrawer( GravityCompat.START );
        else
            super.onBackPressed();
    }
    
    @SuppressLint( "InlinedApi" )
    @TargetApi( 11 )
    private void hideSystemBars()
    {
        // Only applies to Honeycomb devices
        if( !AppData.IS_HONEYCOMB )
            return;
        
        View view = mSurface.getRootView();
        if( view != null )
        {
            if( AppData.IS_KITKAT && mUserPrefs.isImmersiveModeEnabled )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            else
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE ); // == STATUS_BAR_HIDDEN for Honeycomb
        }
    }
    
    @SuppressLint( "ClickableViewAccessibility" )
    @Override
    public boolean onTouch( View v, MotionEvent event )
    {
        int x = (int) event.getX();
        int y = (int) event.getY();
        
        if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_DOWN )
        {
            initialX = x;
            initialY = y;
            dragIndex = TouchMap.UNMAPPED;
            dragging = false;
            dragAsset = "";
            
            if( AppData.IS_KITKAT && mUserPrefs.isImmersiveModeEnabled )
            {
                // ignore edge swipes.
                // unfortunately KitKat lacks a way to do this on its own,
                // so just ignore all touches along the edges.
                // http://stackoverflow.com/questions/20530333/ignore-immersive-mode-swipe
                View view = getWindow().getDecorView();
                if ( y < 10 || y > view.getHeight() - 10 || x < 10 || x > view.getWidth() - 10 )
                    return false;
            }
            
            // Get the N64 index of the button that was pressed
            int index = mTouchscreenMap.getButtonPress( x, y );
            if( index != TouchMap.UNMAPPED )
            {
                dragIndex = index;
                dragAsset = TouchMap.ASSET_NAMES.get( index );
                dragFrame = mTouchscreenMap.getButtonFrame( dragAsset );
            }
            else
            {
                // See if analog was pressed
                Point point = mTouchscreenMap.getAnalogDisplacement( x, y );
                int dX = point.x;
                int dY = point.y;
                float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
                if( mTouchscreenMap.isInCaptureRange( displacement ) )
                {
                    dragAsset = ANALOG;
                    dragFrame = mTouchscreenMap.getAnalogFrame();
                }
            }
            
            dragX = mProfile.getInt( dragAsset + TAG_X, INITIAL_ASSET_POS );
            dragY = mProfile.getInt( dragAsset + TAG_Y, INITIAL_ASSET_POS );
            
            return true;
        }
        else if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_MOVE )
        {
            if ( dragIndex != TouchMap.UNMAPPED || ANALOG.equals(dragAsset) )
            {
                if ( !dragging )
                {
                    int dX = x - initialX;
                    int dY = y - initialY;
                    float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
                    if ( displacement >= 10 )
                        dragging = true;
                }
                if ( !dragging )
                    return false;
                
                // drag this button or analog stick around
                
                // calculate the X and Y percentage
                View view = getWindow().getDecorView();
                int newDragX = ( x - ( initialX - dragFrame.left ) ) * 100/( view.getWidth() - ( dragFrame.right - dragFrame.left ) );
                int newDragY = ( y - ( initialY - dragFrame.top ) ) * 100/( view.getHeight() - ( dragFrame.bottom - dragFrame.top ) );
                
                newDragX = Math.min( Math.max( newDragX, 0 ), 100 );
                newDragY = Math.min( Math.max( newDragY, 0 ), 100 );
                
                if ( newDragX != dragX || newDragY != dragY )
                {
                    dragX = newDragX;
                    dragY = newDragY;
                    mProfile.put( dragAsset + TAG_X, String.valueOf( newDragX ) );
                    mProfile.put( dragAsset + TAG_Y, String.valueOf( newDragY ) );
                    refresh();
                }
            }
        }
        else if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP )
        {
            // if this touch was part of a drag/swipe gesture then don't tap the button
            if ( dragging )
                return false;
            
            // show the editor for the tapped button
            if ( ANALOG.equals( dragAsset ) )
            {
                // play the standard button sound effect
                View view = getWindow().getDecorView();
                view.playSoundEffect( SoundEffectConstants.CLICK );
                
                popupDialog( dragAsset, getString( R.string.controller_analog ), -1 );
            }
            else if( dragIndex != TouchMap.UNMAPPED )
            {
                int index = dragIndex;
                String title = READABLE_NAMES.get( dragIndex );
                
                // D-pad buttons are not holdable
                if( DPAD.equals( dragAsset ) )
                    index = -1;
                
                // play the standard button sound effect
                View view = getWindow().getDecorView();
                view.playSoundEffect( SoundEffectConstants.CLICK );
                
                popupDialog( dragAsset, title, index );
            }
            
            return true;
        }
        
        return false;
    }
    
    @SuppressLint( "InflateParams" )
    private void popupDialog( final String assetName, String title, final int holdableIndex )
    {
        // Get the original position of the asset
        final int initialX = mProfile.getInt( assetName + TAG_X, INITIAL_ASSET_POS );
        final int initialY = mProfile.getInt( assetName + TAG_Y, INITIAL_ASSET_POS );
        
        // Inflate the dialog's main view area
        View view = getLayoutInflater().inflate( R.layout.touchscreen_profile_activity_popup, null );
        
        // Setup the dialog's compound seekbar widgets
        final SeekBarGroup posX = new SeekBarGroup( initialX, view, R.id.seekbarX,
                R.id.buttonXdown, R.id.buttonXup, R.id.textX,
                getString( R.string.touchscreenProfileActivity_horizontalSlider ),
                new SeekBarGroup.Listener()
                {
                    @Override
                    public void onValueChanged( int value )
                    {
                        mProfile.put( assetName + TAG_X, String.valueOf( value ) );
                        refresh();
                    }
                } );
        
        final SeekBarGroup posY = new SeekBarGroup( initialY, view, R.id.seekbarY,
                R.id.buttonYdown, R.id.buttonYup, R.id.textY,
                getString( R.string.touchscreenProfileActivity_verticalSlider ),
                new SeekBarGroup.Listener()
                {
                    @Override
                    public void onValueChanged( int value )
                    {
                        mProfile.put( assetName + TAG_Y, String.valueOf( value ) );
                        refresh();
                    }
                } );
        
        // Setup the auto-holdability checkbox
        CheckBox holdable = (CheckBox) view.findViewById( R.id.checkBox_holdable );
        if( holdableIndex < 0 )
        {
            // This is not a holdable button
            holdable.setVisibility( View.GONE );
        }
        else
        {
            holdable.setChecked( getHoldable( holdableIndex ) );
            holdable.setOnCheckedChangeListener( new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                {
                    setHoldable( holdableIndex, isChecked );
                }
            } );
        }
        
        // Setup the listener for the dialog's bottom buttons (ok, cancel, etc.)
        OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_NEGATIVE )
                {
                    // Revert asset to original position if user cancels
                    posX.revertValue();
                    posY.revertValue();
                }
                else if( which == DialogInterface.BUTTON_NEUTRAL )
                {
                    // Remove the asset from this profile
                    toggleAsset( assetName );
                }
            }
        };
        
        // Create and show the popup dialog
        Builder builder = new Builder( this );
        builder.setTitle( title );
        builder.setView( view );
        builder.setNegativeButton( getString( android.R.string.cancel ), listener );
        builder.setNeutralButton( getString( R.string.touchscreenProfileActivity_remove ), listener );
        builder.setPositiveButton( getString( android.R.string.ok ), listener );
        builder.setCancelable( false );
        builder.create().show();
    }
}
