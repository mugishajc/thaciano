package rw.delasoft.thacianoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PhoneNumberAdapter extends RecyclerView.Adapter<PhoneNumberAdapter.PhoneNumberViewHolder> {

    private List<String> phoneNumbers;
    private OnPhoneNumberClickListener listener;

    public interface OnPhoneNumberClickListener {
        void onPhoneNumberClick(String phoneNumber);
        void onPhoneNumberDelete(String phoneNumber, int position);
    }

    public PhoneNumberAdapter(OnPhoneNumberClickListener listener) {
        this.phoneNumbers = new ArrayList<>();
        this.listener = listener;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
        notifyDataSetChanged();
    }

    public String getPhoneNumberAt(int position) {
        return phoneNumbers.get(position);
    }

    public void removePhoneNumber(int position) {
        phoneNumbers.remove(position);
        notifyItemRemoved(position);
    }

    public void restorePhoneNumber(String phoneNumber, int position) {
        phoneNumbers.add(position, phoneNumber);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public PhoneNumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phone_number, parent, false);
        return new PhoneNumberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneNumberViewHolder holder, int position) {
        String phoneNumber = phoneNumbers.get(position);
        holder.bind(phoneNumber);
    }

    @Override
    public int getItemCount() {
        return phoneNumbers.size();
    }

    class PhoneNumberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPhoneNumber;

        public PhoneNumberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPhoneNumberClick(phoneNumbers.get(position));
                }
            });
        }

        public void bind(String phoneNumber) {
            tvPhoneNumber.setText(phoneNumber);
        }
    }
}
