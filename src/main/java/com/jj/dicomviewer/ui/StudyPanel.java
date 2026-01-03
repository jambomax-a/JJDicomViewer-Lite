package com.jj.dicomviewer.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * StudyPanel - Study（研究）パネル
 * 
 * HOROS-20240407準拠: ViewerController.m 5091-5242行目
 * 
 * Study情報を表示する開閉可能なパネル
 * 番号付きで、クリックするとSeriesサムネイルの表示/非表示が切り替わる
 */
public class StudyPanel extends JPanel {
    
    // HOROS-20240407準拠: ViewerController.m 5184行目
    // [components addObject: [NSString stringWithFormat: @" %d ", (int) curStudyIndexAll+1]];
    private int studyIndex;
    
    // HOROS-20240407準拠: ViewerController.m 4857行目
    // [curStudy setHidden: ![curStudy isHidden]];
    private boolean isHidden = false;
    
    // HOROS-20240407準拠: ViewerController.m 5098行目
    // [cell setRepresentedObject:[O2ViewerThumbnailsMatrixRepresentedObject object:curStudy children:[seriesArray objectAtIndex:curStudyIndex]]];
    private Object study; // Studyオブジェクト
    private List<Object> seriesList; // Seriesのリスト
    
    // Studyパネルのヘッダー（クリック可能）
    private JButton headerButton;
    
    // Seriesサムネイルを表示するパネル
    private JPanel seriesPanel;
    
    // HOROS-20240407準拠: ViewerController.m 5099行目
    // [cell setAction: @selector(matrixPreviewSwitchHidden:)];
    private ActionListener switchHiddenAction;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: ViewerController.m 5091-5242行目
     */
    public StudyPanel(int studyIndex, Object study, List<Object> seriesList) {
        super();
        this.studyIndex = studyIndex;
        this.study = study;
        this.seriesList = seriesList != null ? seriesList : new ArrayList<>();
        
        // HOROS-20240407準拠: パネルが右寄りにならないように、左寄せレイアウトを使用
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // HOROS-20240407準拠: ViewerController.m 5184-5192行目
        // Study情報を表示するヘッダーボタンを作成
        headerButton = new JButton();
        // HOROS-20240407準拠: 複数行テキストを表示するため、HTMLを使用
        headerButton.setHorizontalAlignment(SwingConstants.CENTER);
        headerButton.setVerticalAlignment(SwingConstants.CENTER);
        updateHeaderText();
        
        // HOROS-20240407準拠: ViewerController.m 5099行目
        // [cell setAction: @selector(matrixPreviewSwitchHidden:)];
        headerButton.addActionListener(e -> {
            if (switchHiddenAction != null) {
                switchHiddenAction.actionPerformed(e);
            }
            toggleHidden();
        });
        
        add(headerButton);
        
        // Seriesサムネイルを表示するパネル
        seriesPanel = new JPanel();
        seriesPanel.setLayout(new BoxLayout(seriesPanel, BoxLayout.Y_AXIS));
        seriesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        seriesPanel.setBorder(new EmptyBorder(0, 10, 0, 0)); // インデント
        add(seriesPanel);
        
        // HOROS-20240407準拠: ViewerController.m 5016行目
        // if( [s isHidden] == NO)
        //     i += [[seriesArray lastObject] count];
        // Studyが非表示でない場合、Seriesサムネイルを表示
        // 初期状態では表示（HOROS-20240407では、Studyは通常表示されている）
        setHidden(false);
    }
    
    /**
     * ヘッダーテキストを更新
     * HOROS-20240407準拠: ViewerController.m 5183-5195行目
     */
    private void updateHeaderText() {
        // HOROS-20240407準拠: ViewerController.m 5183-5192行目
        // NSMutableArray* components = [NSMutableArray array];
        // [components addObject: [NSString stringWithFormat: @" %d ", (int) curStudyIndexAll+1]];
        // [components addObject: @""];
        // if (patName.length) [components addObject:patName];
        // if (name.length) [components addObject:name];
        // if ([curStudy date]) [components addObject:[[NSUserDefaults dateTimeFormatter] stringFromDate:[curStudy date]]];
        // [components addObject:[NSString stringWithFormat:NSLocalizedString(@"%@: %@", @"semicolon separator for spacing"), modality, N2SingularPluralCount([series count], NSLocalizedString(@"series", @"one series, singular"), NSLocalizedString(@"series", @"zero or 2 or more series, plural"))]];
        // if (stateText.length) [components addObject:stateText];
        // if (comment.length) [components addObject:comment];
        // if (action.length) [components addObject:[NSString stringWithFormat:@"\r%@", action]];
        
        java.util.List<String> components = new java.util.ArrayList<>();
        
        // HOROS-20240407準拠: ViewerController.m 5184行目
        // 番号
        components.add(" " + (studyIndex + 1) + " ");
        
        // HOROS-20240407準拠: ViewerController.m 5185行目
        // 空行
        components.add("");
        
        // HOROS-20240407準拠: ViewerController.m 5123-5192行目
        // Study情報を取得
        // HOROS-20240407準拠: ViewerController.m 5123行目
        // NSString *name = [[curStudy valueForKey:@"studyName"] stringByTruncatingToLength: 34];
        // 注意: "studyName"はStudy Descriptionを指す
        String studyDescription = "";
        String modality = "OT:";
        String date = "";
        int seriesCount = seriesList != null ? seriesList.size() : 0;
        String stateText = "";
        String comment = "";
        String action = isHidden ? "Show Series" : "Hide Series"; // HOROS-20240407準拠: ViewerController.m 5164-5167行目
        
        // HOROS-20240407準拠: ViewerController.m 5123行目
        // 実際のStudyオブジェクトから情報を取得
        if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
            com.jj.dicomviewer.model.DicomStudy dicomStudy = (com.jj.dicomviewer.model.DicomStudy) study;
            
            // HOROS-20240407準拠: ViewerController.m 5123行目
            // NSString *name = [[curStudy valueForKey:@"studyName"] stringByTruncatingToLength: 34];
            studyDescription = dicomStudy.getStudyName();
            if (studyDescription == null || studyDescription.isEmpty()) {
                studyDescription = "Unnamed"; // HOROS-20240407準拠: 空の場合は"Unnamed"
            } else if (studyDescription.length() > 34) {
                studyDescription = studyDescription.substring(0, 34);
            }
            
            // HOROS-20240407準拠: ViewerController.m 5135-5137行目
            modality = dicomStudy.getModality();
            if (modality == null || modality.isEmpty()) {
                modality = "OT:";
            }
            
            // HOROS-20240407準拠: ViewerController.m 5188行目
            if (dicomStudy.getDate() != null) {
                // TODO: 日付フォーマッターを使用
                date = dicomStudy.getDate().toString();
            }
            
            // HOROS-20240407準拠: ViewerController.m 5125-5128行目
            Integer stateIndex = dicomStudy.getStateText();
            if (stateIndex != null && stateIndex.intValue() > 0) {
                // TODO: BrowserController.statesArrayから取得
                stateText = ""; // TODO: 実装
            }
            
            // HOROS-20240407準拠: ViewerController.m 5129-5133行目
            comment = dicomStudy.getComment();
            if (comment == null) {
                comment = "";
            } else if (comment.length() > 32) {
                comment = comment.substring(0, 32);
            }
        } else {
            // 仮のデータ（Studyオブジェクトが取得できない場合）
            studyDescription = "Unnamed";
        }
        
        // HOROS-20240407準拠: ViewerController.m 5186-5192行目
        // 患者名は条件付きで表示（現在はスキップ）
        // if (patName.length) [components addObject:patName];
        
        // HOROS-20240407準拠: ViewerController.m 5187行目
        // if (name.length) [components addObject:name];
        // nameはStudy Description（studyName）
        if (studyDescription != null && !studyDescription.isEmpty()) {
            components.add(studyDescription);
        }
        
        if (date != null && !date.isEmpty()) {
            components.add(date);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5189行目
        // モダリティとSeries数
        String seriesText = seriesCount == 1 ? "series" : "series";
        components.add(modality + ": " + seriesCount + " " + seriesText);
        
        if (stateText != null && !stateText.isEmpty()) {
            components.add(stateText);
        }
        
        if (comment != null && !comment.isEmpty()) {
            components.add(comment);
        }
        
        if (action != null && !action.isEmpty()) {
            components.add("\n" + action);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5195行目
        // [components componentsJoinedByString:@"\r"]
        // HOROS-20240407準拠: ViewerController.m 5198-5211行目
        // 番号部分を太字で背景色付きにする
        // Java Swingでは、HTMLを使用してスタイルを設定
        StringBuilder htmlText = new StringBuilder();
        htmlText.append("<html><body style='text-align:center;'>");
        
        // HOROS-20240407準拠: ViewerController.m 5198-5211行目
        // 番号部分を太字で背景色付きにする
        // NSFont boldSystemFontOfSize: viewerNumberFont
        // NSBackgroundColorAttributeName
        htmlText.append("<span style='font-weight:bold;background-color:#FFE4B5;'>");
        htmlText.append(components.get(0)); // 番号
        htmlText.append("</span>");
        
        // 残りの情報を追加
        for (int i = 1; i < components.size(); i++) {
            String component = components.get(i);
            if (component.isEmpty()) {
                htmlText.append("<br>");
            } else {
                htmlText.append("<br>").append(component);
            }
        }
        
        htmlText.append("</body></html>");
        
        headerButton.setText(htmlText.toString());
    }
    
    /**
     * 表示/非表示を切り替え
     * HOROS-20240407準拠: ViewerController.m 4857行目
     */
    public void toggleHidden() {
        setHidden(!isHidden);
    }
    
    /**
     * 表示/非表示を設定
     * HOROS-20240407準拠: ViewerController.m 4857行目
     */
    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
        seriesPanel.setVisible(!hidden);
        // HOROS-20240407準拠: ViewerController.m 5164-5167行目
        // アクションテキストを更新
        updateHeaderText();
        revalidate();
        repaint();
    }
    
    /**
     * 非表示かどうかを取得
     * HOROS-20240407準拠: ViewerController.m 5164行目
     */
    public boolean isHidden() {
        return isHidden;
    }
    
    /**
     * Studyオブジェクトを取得
     */
    public Object getStudy() {
        return study;
    }
    
    /**
     * Seriesリストを取得
     */
    public List<Object> getSeriesList() {
        return seriesList;
    }
    
    /**
     * Seriesサムネイルパネルを取得
     */
    public JPanel getSeriesPanel() {
        return seriesPanel;
    }
    
    /**
     * スイッチアクションを設定
     * HOROS-20240407準拠: ViewerController.m 5099行目
     */
    public void setSwitchHiddenAction(ActionListener action) {
        this.switchHiddenAction = action;
    }
}
