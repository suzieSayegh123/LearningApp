package com.example.bagrutTel.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bagrutTel.R;
import com.example.bagrutTel.adapters.AdapterComments;
import com.example.bagrutTel.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    //to get detail of user and post
    String hisUid,myUid, myEmail,myName,myDp,
    postId,pLikes,hisDp,hisName,pImage;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    //progress bar
    ProgressDialog pd;

    //views
    ImageView uPictureIv,pImageIv;
    TextView uNameTv,pTimeTv,pTitleTv,pDescriptionTv,pLikesTv,pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn,shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    //add comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        //Actionbar and iys properties
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.postDetail);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        //init views
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);


        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        setLikes();

        //set subtitle of actionbar
        actionBar.setSubtitle("SignedIn as: "+ myName);

        loadComments();

        //send comment button click
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });
        
        //like button click handle
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        //more button click handle
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        //share button click handle
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pTitle = pTitleTv.getText().toString().trim();
                String pDescription = pDescriptionTv.getText().toString().trim();

                BitmapDrawable bitmapDrawable = (BitmapDrawable)pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle,pDescription);
                }
                else{
                    //post with image


                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pDescription,bitmap);

                }
            }
        });

        //click like count to start PostLikedByActivity,and pass the post id
        pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostDetailActivity.this, PostLikedByActivity.class);
                intent.putExtra("postId", postId);
                startActivity(intent);
            }
        });
    }

    private void addToHisNotifications(String hisUid,String pId,String notification){
        String timestamp = ""+System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId",pId);
        hashMap.put("timestamp",timestamp);
        hashMap.put("pUid",hisUid);
        hashMap.put("notification",notification);
        hashMap.put("sUid",myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added successfully
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                    }
                });
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        String shareBody = pTitle +"\n"+pDescription;

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT,R.string.subjectHere);//in case email app
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);//text to share
        startActivity(Intent.createChooser(sIntent,getString(R.string.shareViaI))); //message to show in dialog
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        String shareBody = pTitle +"\n"+pDescription;

        //save image in cache,get saved image uri
        Uri uri = saveImageToShare(bitmap);

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,R.string.subjectHere);
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,getString(R.string.shareViaIn)));


    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();//create if not exixts
            File file = new File(imageFolder,"shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,"com.example.bagrutTel.fileprovider",
                    file);

        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
        return uri;

    }

    private void loadComments() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        //set layout to recycler view
        recyclerView.setLayoutManager(layoutManager);

        //init comments list
        commentList = new ArrayList<>();

        //path of the post,to get its comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ModelComment modelComment = ds.getValue(ModelComment.class);

                    commentList.add(modelComment);

                    //pass myUid and postId as parameter of constructor of CommentAdapter


                    //setup adapter
                    adapterComments = new AdapterComments(getApplicationContext(),commentList,myUid,postId);
                    //set adapter
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions() {

        PopupMenu popupMenu=new PopupMenu(this,moreBtn, Gravity.END);

        //show delete option in only currently users posts
        if (hisUid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,R.string.delete);
            popupMenu.getMenu().add(Menu.NONE,1,0,R.string.edit);

        }
        //item click listner
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id==0){
                    //delete is clicked
                    beginDelete();
                }
                else if (id==1){
                    //edit is clicked
                    //start AddPostActivity with key "edit post" and the id of the post clicked
                    Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",postId);
                    startActivity(intent);

                }
                return false;
            }
        });
        //show menu
        popupMenu.show();

    }

    private void beginDelete() {

        //post can be with or without image

        if (pImage.equals("noImage")){
            //post is without image
            deleteWithoutImage();
        }
        else{
            //post
            deleteWithImage();
        }

    }

    private void deleteWithImage() {

        //progress bar
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.deletingM));

        //delete image using url,delete from database using post id

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image delted,now delete database

                        Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                        fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()){
                                    ds.getRef().removeValue();//remove values from firebase where pid mathces
                                }
                                //deleted
                                Toast.makeText(PostDetailActivity.this, R.string.deletedSuccessfully, Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed,cant go further
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteWithoutImage() {

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.deletingM));

        Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ds.getRef().removeValue();//remove values from firebase where pid mathces
                }
                //deleted
                Toast.makeText(PostDetailActivity.this, R.string.deletedSuccessfully, Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setLikes() {
        //when the details of post is loading,also check if current user has liked it or not
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)){
                    //user has liked this post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    likeBtn.setText(R.string.liked);
                }
                else{
                    //user has not liked this post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    likeBtn.setText(R.string.like);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void likePost() {
        //get total number of likes for the post, whose like button clicked
        //if currently signed in user has not liked it befor
        //increase value by 1,otherwise decrease value by 1
        mProcessLike = true;
        //get id of the post clicked
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike){
                    if (dataSnapshot.child(postId).hasChild(myUid)){
                        //already liked,so remove like
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;


                    }
                    else {
                        //not liked,like it
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;

                        if (!hisUid.equals(myUid)) {
                            addToHisNotifications("" + hisUid, "" + postId, getString(R.string.likedYourPost));
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment() {
        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.addingComment));

        //get data from comment editText
        String comment = commentEt.getText().toString().trim();
        //validate
        if (TextUtils.isEmpty(comment)){
            //no value is entered
            Toast.makeText(this, R.string.commentIsEmpty, Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStamp = String.valueOf(System.currentTimeMillis());

        //each post will have a child "Comments" that will contain comments of that post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        //put info in hashmap
        hashMap.put("cId",timeStamp);
        hashMap.put("comment",comment);
        hashMap.put("timestamp",timeStamp);
        hashMap.put("uid",myUid);
        hashMap.put("uEmail",myEmail);
        hashMap.put("uDp",myDp);
        hashMap.put("uName",myName);

        //put this data in db
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, R.string.commentAdded, Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                        if (!hisUid.equals(myUid)) {
                            addToHisNotifications("" + hisUid, "" + postId, getString(R.string.commentedonpost));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed, not added
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        
                    }
                });

    }

    private void updateCommentCount() {
        //whenever user adds comment increase the comment count as we did for like count
        mProcessComment = true;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment){
                    String comments = ""+dataSnapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments)+1;
                    ref.child("pComments").setValue(""+newCommentVal);
                    mProcessComment = false;

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void loadUserInfo() {
        //get current user info
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myName = ""+ds.child("name").getValue();
                    myDp = ""+ds.child("image").getValue();

                    //set data
                    try {
                        //if image is received then set
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadPostInfo() {
        //get post using the id of the post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    //get data
                    String pTitle = ""+ds.child("pTitle").getValue();
                    String pDescr = ""+ds.child("pDescr").getValue();
                    pLikes = ""+ds.child("pLikes").getValue();
                    String pTimeStamp = ""+ds.child("pTime").getValue();
                    pImage = ""+ds.child("pImage").getValue();
                    hisDp = ""+ds.child("uDp").getValue();
                    hisUid = ""+ds.child("uid").getValue();
                    String uEmail = ""+ds.child("uEmail").getValue();
                    hisName = ""+ds.child("uName").getValue();
                    String commentCount = ""+ds.child("pComments").getValue();

                    //convert timestamp to proper format
                    //convert timestamp to dd/mm/yyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

                    //set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes+" "+getString(R.string.likesS));
                    pTimeTv.setText(pTime);
                    pCommentsTv.setText(commentCount + " "+getString(R.string.commentsS));

                    uNameTv.setText(hisName);

                    //set image of the user who posted
                    //if there is no image then hide image view
                    if (pImage.equals("noImage")){
                        //hide imageview
                        pImageIv.setVisibility(View.GONE);
                    }
                    else {
                        //show imageview
                        pImageIv.setVisibility(View.VISIBLE);
                        try {
                            Picasso.get().load(pImage).into(pImageIv);
                        }
                        catch (Exception e){

                        }
                    }

                    //set user image in comment part
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
                    }




                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null){
            //user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();
            myName = user.getDisplayName();
        }
        else {
            //user not signed in,go to main activity
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        //hide some menu items
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
