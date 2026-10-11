//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
package com.mx.kylgis.pos.payment;

import com.mx.kylgis.pos.editor.JEditorCurrencyPositive;
import com.mx.kylgis.pos.editor.JEditorKeys;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.util.RoundUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PaymentPanelBasic extends javax.swing.JPanel implements PaymentPanel {

    private double m_dTotal;
    private double m_dAvailableTotal;
    private String m_sTransactionID;
    private JPaymentNotifier m_notifier;
    private JEditorCurrencyPositive m_jAmount;
    private JEditorKeys m_jKeys;

    /**
     * Creates new form PaymentPanelSimple
     */
    public PaymentPanelBasic(JPaymentNotifier notifier) {

        m_notifier = notifier;
        initComponents();
        initPartialAmountControls();
    }

    private void initPartialAmountControls() {
        m_jAmount = new JEditorCurrencyPositive();
        m_jKeys = new JEditorKeys();
        setPreferredSize(new Dimension(620, 255));
        m_jAmount.setPreferredSize(new Dimension(150, 30));
        m_jKeys.setPreferredSize(new Dimension(290, 225));
        m_jAmount.addEditorKeys(m_jKeys);
        m_jAmount.addPropertyChangeListener("Edition", new RecalculateAmount());

        JPanel amountRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        amountRow.add(new JLabel(AppLocal.getIntString("label.payment.cardamount")));
        amountRow.add(m_jAmount);

        removeAll();
        setLayout(new BorderLayout(8, 4));
        add(amountRow, BorderLayout.NORTH);
        add(jLabel1, BorderLayout.CENTER);
        add(m_jKeys, BorderLayout.EAST);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void activate(String sTransaction, double dTotal) {

        m_sTransactionID = sTransaction;
        m_dAvailableTotal = dTotal;
        m_dTotal = dTotal;

        jLabel1.setText(
                m_dAvailableTotal > 0.0
                ? AppLocal.getIntString("message.paymentgatewayext")
                : AppLocal.getIntString("message.paymentgatewayextrefund"));

        if (m_dAvailableTotal > 0.0) {
            m_jAmount.setEnabled(true);
            m_jKeys.setEnabled(true);
            m_jAmount.setDoubleValue(RoundUtils.round(m_dAvailableTotal));
            m_jAmount.activate();
            recalculateAmount();
        } else {
            // Mantener el flujo histórico de reembolso: no fraccionarlo aquí.
            m_jAmount.reset();
            m_jAmount.setEnabled(false);
            m_jKeys.setEnabled(false);
            m_notifier.setStatus(true, true);
        }
    }

    private void recalculateAmount() {
        if (m_dAvailableTotal <= 0.0) {
            m_dTotal = m_dAvailableTotal;
            m_notifier.setStatus(true, true);
            return;
        }

        Double value = m_jAmount.getDoubleValue();
        if (value == null) {
            m_dTotal = 0.0;
            m_notifier.setStatus(false, false);
            return;
        }

        m_dTotal = RoundUtils.round(value);
        int compare = RoundUtils.compare(m_dTotal, m_dAvailableTotal);
        boolean valid = m_dTotal > 0.0 && compare <= 0;
        m_notifier.setStatus(valid, valid && compare == 0);
    }

    private boolean isPositiveAmountValid() {
        return m_dAvailableTotal > 0.0
                && m_dTotal > 0.0
                && RoundUtils.compare(m_dTotal, m_dAvailableTotal) <= 0;
    }

    @Override
    public PaymentInfoMagcard getPaymentInfoMagcard() {

        if (m_dAvailableTotal > 0.0) {
            recalculateAmount();
            if (!isPositiveAmountValid()) {
                throw new IllegalStateException(
                        AppLocal.getIntString("message.payment.cardamountinvalid"));
            }
            return new PaymentInfoMagcard(
                    "",
                    "",
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    m_sTransactionID,
                    m_dTotal);
        } else {
            return new PaymentInfoMagcardRefund(
                    "",
                    "",
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    m_sTransactionID,
                    m_dTotal);
        }
    }

    private class RecalculateAmount implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            recalculateAmount();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();

        add(jLabel1);

    }
    // </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
