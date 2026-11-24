package com.example.uollostfound;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Puts each LostItem into an item_lost card. Open posts are white cards with a coloured
 * Lost / Found badge; Returned posts turn into flat grey cards with a check mark.
 */
public class LostItemAdapter extends RecyclerView.Adapter<LostItemAdapter.LostItemViewHolder> {

    /** Same pattern as Lab Task 2: the activity decides what a tap on a card does. */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    static final String STATUS_RETURNED = "Returned";
    static final String TYPE_FOUND = "Found";

    private final ArrayList<LostItem> items;
    private final OnItemClickListener listener;

    public LostItemAdapter(ArrayList<LostItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LostItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lost, parent, false);
        return new LostItemViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull LostItemViewHolder holder, int position) {
        LostItem item = items.get(position);
        Context context = holder.itemView.getContext();

        boolean returned = STATUS_RETURNED.equals(item.getStatus());
        boolean found = TYPE_FOUND.equals(item.getType());

        holder.tvItemName.setText(item.getItemName());
        holder.tvType.setText(item.getType());
        holder.tvLocation.setText(item.getLocation());
        holder.tvContact.setText(item.getContact());
        holder.tvPosted.setText(postedAgo(context, item.getId()));

        if (returned) {
            int muted = ContextCompat.getColor(context, R.color.returned_text);
            holder.cardItem.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.returned_surface));
            holder.cardItem.setCardElevation(0f);
            holder.tvItemName.setTextColor(muted);
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_returned);
            holder.tvType.setTextColor(muted);
            holder.tvStatus.setText(R.string.status_returned);
            holder.tvStatus.setTextColor(muted);
            setStatusIcon(holder.ivStatus, R.drawable.ic_check, 14, muted);
        } else {
            int open = ContextCompat.getColor(context, R.color.status_open);
            holder.cardItem.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface));
            holder.cardItem.setCardElevation(
                    context.getResources().getDisplayMetrics().density * 3f);
            holder.tvItemName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.tvType.setBackgroundResource(
                    found ? R.drawable.bg_badge_found : R.drawable.bg_badge_lost);
            holder.tvType.setTextColor(ContextCompat.getColor(context,
                    found ? R.color.found_text : R.color.lost_text));
            holder.tvStatus.setText(item.getStatus() == null
                    ? context.getString(R.string.status_open) : item.getStatus());
            holder.tvStatus.setTextColor(open);
            setStatusIcon(holder.ivStatus, R.drawable.bg_dot, 8, open);
        }

        // TalkBack reads the whole card as one sentence and says what a tap does
        holder.itemView.setContentDescription(context.getString(
                returned ? R.string.cd_card_returned : R.string.cd_card_open,
                item.getItemName(), item.getType(), item.getLocation(), item.getContact()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static void setStatusIcon(ImageView icon, int drawable, int sizeDp, int color) {
        float density = icon.getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        lp.width = Math.round(sizeDp * density);
        lp.height = Math.round(sizeDp * density);
        icon.setLayoutParams(lp);
        icon.setImageResource(drawable);
        icon.setImageTintList(ColorStateList.valueOf(color));
    }

    /**
     * A Firebase push() key starts with 8 characters that encode the time the key was made,
     * so the card can say "5 min ago" without storing an extra field.
     */
    static String postedAgo(Context context, String pushKey) {
        long time = pushKeyTime(pushKey);
        if (time <= 0) {
            return "";
        }
        long now = System.currentTimeMillis();
        if (now - time < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.posted_just_now);
        }
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }

    private static final String PUSH_CHARS =
            "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";

    static long pushKeyTime(String key) {
        if (key == null || key.length() < 8) {
            return -1;
        }
        long time = 0;
        for (int i = 0; i < 8; i++) {
            int value = PUSH_CHARS.indexOf(key.charAt(i));
            if (value < 0) {
                return -1;
            }
            time = time * 64 + value;
        }
        return time;
    }

    /** Static inner ViewHolder: finds the card's views once and forwards taps. */
    static class LostItemViewHolder extends RecyclerView.ViewHolder {
        final CardView cardItem;
        final TextView tvItemName, tvType, tvLocation, tvContact, tvStatus, tvPosted;
        final ImageView ivStatus;

        LostItemViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardItem);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvType = itemView.findViewById(R.id.tvType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPosted = itemView.findViewById(R.id.tvPosted);
            ivStatus = itemView.findViewById(R.id.ivStatus);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}
