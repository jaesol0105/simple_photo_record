# commit 내역

## 2023-06-14
#### ● item_record.xml (recyclerView) ui 변경

#### ● recyclerView item longclick시 fab 비활성화, appBar에 선택 개수 표시

#### ● datepicker/timepicker의 날짜를 텍스트로 표시 + ui 변경

#### ● 정렬 dialog, alert dialog들 bottomsheetdialog로 변경

#### ● photo dialog 이미지 두 손가락으로 zoom-in
&ensp; implementation 'com.davemorrissey.labs:subsampling-scale-image-view:3.10.0'

## 2023-06-04
#### ● 다중삭제 기능
&ensp; recycleView의 item을 길게 누를경우(<b>LongClick</b>) 체크박스가 활성화되며, 우측 상단의 menu가 삭제 아이콘으로 변경된다.

## 2023-05-25
#### ● 레코드 생성 fab
&ensp; menu를 통해 Record 생성 -> <b>fab</b>을 통해 Record 생성

#### ● 레코드 정렬 기능
&ensp; menu를 통해 정렬 기준을 설정하는 dialog 출력, 정렬 기준은 <b>sharedPreference</b>로 저장

#### ● 날짜 설정 기능
&ensp; DateTimePickerFragment : Record의 날짜정보를 기반으로 custom dialog를 생성<br>
&ensp; Custom Dialog : spinner 형태의 <b>datePicker와 timePicker를 결합</b> (bottomSheetDialog)

#### ● 일괄 삭제 기능
&ensp; 좌측 상단의 Navigation Menu를 통해 데이터 관리 탭에서 레코드를 일괄 삭제
